package skinemsya.vse.ru.notifications.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.events.application.EventAccessPort;
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

import java.time.Instant;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final TelegramBotClient telegramBotClient;
    private final EventAccessPort eventAccessPort;
    private final EventParticipantRepository eventParticipantRepository;
    private final GroupService groupService;
    private final UserService userService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            TelegramBotClient telegramBotClient,
            EventAccessPort eventAccessPort,
            EventParticipantRepository eventParticipantRepository,
            GroupService groupService,
            UserService userService
    ) {
        this.notificationRepository = notificationRepository;
        this.telegramBotClient = telegramBotClient;
        this.eventAccessPort = eventAccessPort;
        this.eventParticipantRepository = eventParticipantRepository;
        this.groupService = groupService;
        this.userService = userService;
    }

    @Override
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
    public void sendToGroupChat(long telegramChatId, NotificationType type, String message, long eventId) {
        telegramBotClient.sendMessageWithOpenAppButton(
                telegramChatId,
                message,
                "Скинуть",
                "supergroup",
                TelegramStartParam.forEvent(eventId)
        );
    }

    @Override
    public void remindIncompleteSelections(long eventId, long requesterId) {
        eventAccessPort.requireParticipant(eventId, requesterId);
        long groupId = eventAccessPort.getEventGroupId(eventId);
        var group = groupService.findById(groupId).orElseThrow();
        if (group.telegramChatId() == null) {
            return;
        }

        var participants = eventParticipantRepository.findByEventId(eventId);
        for (var participant : participants) {
            if (participant.getSelectionCompletedAt() != null) {
                continue;
            }
            var user = userService.findById(participant.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            String username = user.telegramUsername() != null ? user.telegramUsername() : user.displayName();
            String text = "Ждём выбор блюд от @" + username;
            sendToGroupChat(group.telegramChatId(), NotificationType.REMINDER, text, eventId);
            break;
        }
    }

    private static String toJsonPayload(String message) {
        var escaped = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
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
                entity.getCreatedAt()
        );
    }

    static String formatRubles(long kopecks) {
        return String.valueOf(kopecks / 100);
    }
}
