package skinemsya.vse.ru.notifications.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.infrastructure.telegram.TelegramStartParam;
import skinemsya.vse.ru.notifications.domain.Notification;
import skinemsya.vse.ru.notifications.domain.NotificationStatus;
import skinemsya.vse.ru.notifications.domain.NotificationType;
import skinemsya.vse.ru.notifications.infrastructure.persistence.NotificationEntity;
import skinemsya.vse.ru.notifications.infrastructure.persistence.NotificationRepository;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.User;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final TelegramBotClient telegramBotClient;
    private final EventAccessPort eventAccessPort;
    private final EventParticipantRepository eventParticipantRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final DebtService debtService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            TelegramBotClient telegramBotClient,
            EventAccessPort eventAccessPort,
            EventParticipantRepository eventParticipantRepository,
            GroupService groupService,
            UserService userService,
            DebtService debtService) {
        this.notificationRepository = notificationRepository;
        this.telegramBotClient = telegramBotClient;
        this.eventAccessPort = eventAccessPort;
        this.eventParticipantRepository = eventParticipantRepository;
        this.groupService = groupService;
        this.userService = userService;
        this.debtService = debtService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification send(long userId, NotificationType type, String payload) {
        var entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setType(type);
        entity.setPayload(toJsonPayload(payload));
        entity.setStatus(NotificationStatus.PENDING);
        entity.setCreatedAt(Instant.now());

        try {
            var user = userService.findById(userId).orElseThrow();
            telegramBotClient.sendMessage(user.telegramUserId(), payload);
            entity.setStatus(NotificationStatus.SENT);
            entity.setSentAt(Instant.now());
        } catch (RuntimeException ex) {
            entity.setStatus(NotificationStatus.FAILED);
        }

        entity = notificationRepository.saveAndFlush(entity);
        return toDomain(entity);
    }

    @Override
    public void sendToEventParticipants(long eventId, NotificationType type, String payload) {
        for (long userId : eventAccessPort.getParticipantUserIds(eventId)) {
            send(userId, type, payload);
        }
    }

    @Override
    public void sendToGroupChat(long telegramChatId, NotificationType type, String message) {
        telegramBotClient.sendMessage(telegramChatId, message);
    }

    @Override
    public void sendToGroupChat(long telegramChatId, NotificationType type, String message, long eventId) {
        telegramBotClient.sendMessageWithOpenAppButton(
                telegramChatId, message, actionButtonLabel(type), "supergroup", TelegramStartParam.forEvent(eventId));
    }

    @Override
    public void remindIncompleteSelections(long eventId, long requesterId) {
        eventAccessPort.requireParticipant(eventId, requesterId);
        long groupId = eventAccessPort.getEventGroupId(eventId);
        var group = groupService.findById(groupId).orElseThrow();
        if (group.telegramChatId() == null) {
            return;
        }

        var status = eventAccessPort.getStatus(eventId);
        List<String> mentions;
        String prefix;
        if (status == EventStatus.DISTRIBUTION) {
            mentions = mentionsForIncompleteSelections(eventId);
            prefix = "Ждём выбор позиций от ";
        } else if (status == EventStatus.CALCULATED) {
            mentions = mentionsForUnpaidDebts(eventId);
            prefix = "Ждём перевод от ";
        } else {
            return;
        }
        if (mentions.isEmpty()) {
            return;
        }
        sendToGroupChat(group.telegramChatId(), NotificationType.REMINDER, prefix + formatMentions(mentions), eventId);
    }

    private List<String> mentionsForIncompleteSelections(long eventId) {
        long payerId = eventAccessPort.getPayerId(eventId);
        List<String> mentions = new ArrayList<>();
        for (var participant : eventParticipantRepository.findByEventId(eventId)) {
            if (participant.getUserId() == payerId || participant.getSelectionCompletedAt() != null) {
                continue;
            }
            mentionOf(participant.getUserId()).ifPresent(mentions::add);
        }
        return mentions;
    }

    private List<String> mentionsForUnpaidDebts(long eventId) {
        List<String> mentions = new ArrayList<>();
        for (var debt : debtService.findByEvent(eventId)) {
            if (debt.status() != DebtStatus.UNPAID) {
                continue;
            }
            mentionOf(debt.debtorId()).ifPresent(mentions::add);
        }
        return mentions;
    }

    private Optional<String> mentionOf(long userId) {
        return userService.findById(userId).map(NotificationServiceImpl::mentionOf);
    }

    static String mentionOf(User user) {
        var username = user.telegramUsername();
        if (username != null && !username.isBlank()) {
            return username.startsWith("@") ? username : "@" + username;
        }
        return user.displayName();
    }

    static String formatMentions(List<String> mentions) {
        if (mentions.isEmpty()) {
            return "";
        }
        if (mentions.size() == 1) {
            return mentions.getFirst();
        }
        var leading = String.join(", ", mentions.subList(0, mentions.size() - 1));
        return leading + " и " + mentions.getLast();
    }

    private static String toJsonPayload(String message) {
        var escaped = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"message\":\"" + escaped + "\"}";
    }

    private static Notification toDomain(NotificationEntity entity) {
        return new Notification(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getSentAt(),
                entity.getCreatedAt());
    }

    static String formatRubles(long kopecks) {
        return String.valueOf(kopecks / 100);
    }

    static String actionButtonLabel(NotificationType type) {
        return type == NotificationType.EVENT_COMPLETED ? "Итоги" : "Скинуть";
    }
}
