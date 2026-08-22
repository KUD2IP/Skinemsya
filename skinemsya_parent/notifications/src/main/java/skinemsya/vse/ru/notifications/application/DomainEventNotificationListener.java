package skinemsya.vse.ru.notifications.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import skinemsya.vse.ru.common.event.DebtorConfirmed;
import skinemsya.vse.ru.common.event.EventCompleted;
import skinemsya.vse.ru.common.event.EventSentToDistribution;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.notifications.domain.NotificationType;
import skinemsya.vse.ru.users.application.UserService;

@Component
public class DomainEventNotificationListener {

    private final NotificationService notificationService;
    private final EventAccessPort eventAccessPort;
    private final GroupService groupService;
    private final UserService userService;

    public DomainEventNotificationListener(
            NotificationService notificationService,
            EventAccessPort eventAccessPort,
            GroupService groupService,
            UserService userService) {
        this.notificationService = notificationService;
        this.eventAccessPort = eventAccessPort;
        this.groupService = groupService;
        this.userService = userService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventSentToDistribution(EventSentToDistribution event) {
        var payer = userService.findById(event.payerId()).orElse(null);
        String payerName = payer != null ? payer.displayName() : "Участник";
        String total = NotificationServiceImpl.formatRubles(event.totalKopecks());
        String message =
                payerName + " запустил сбор «" + event.eventTitle() + "» на " + total + " ₽. Выберите свои позиции";

        groupService.findById(event.groupId()).ifPresent(group -> {
            if (group.telegramChatId() != null) {
                notificationService.sendToGroupChat(
                        group.telegramChatId(), NotificationType.DISTRIBUTION_STARTED, message, event.eventId());
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDebtorConfirmed(DebtorConfirmed event) {
        String name = event.debtorName() != null && !event.debtorName().isBlank() ? event.debtorName() : "Участник";
        String message = name + " скинул";
        long groupId = eventAccessPort.getEventGroupId(event.eventId());
        groupService.findById(groupId).ifPresent(group -> {
            if (group.telegramChatId() != null) {
                notificationService.sendToGroupChat(group.telegramChatId(), NotificationType.PAYMENT_PENDING, message);
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventCompleted(EventCompleted event) {
        long groupId = event.groupId();
        String message = "Сбор «" + event.eventTitle() + "» закрыт. Все скинули!";
        groupService.findById(groupId).ifPresent(group -> {
            if (group.telegramChatId() != null) {
                notificationService.sendToGroupChat(
                        group.telegramChatId(), NotificationType.EVENT_COMPLETED, message, event.eventId());
            }
        });
    }
}
