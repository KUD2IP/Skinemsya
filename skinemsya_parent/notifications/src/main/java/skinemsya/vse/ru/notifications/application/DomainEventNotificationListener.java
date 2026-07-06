package skinemsya.vse.ru.notifications.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import skinemsya.vse.ru.common.event.DebtorConfirmed;
import skinemsya.vse.ru.common.event.DebtsCalculated;
import skinemsya.vse.ru.common.event.EventCompleted;
import skinemsya.vse.ru.common.event.EventSentToDistribution;
import skinemsya.vse.ru.common.event.PaymentDisputed;
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
    public void onEventSentToDistribution(EventSentToDistribution event) {
        var payer = userService.findById(event.payerId()).orElse(null);
        String payerName = payer != null ? payer.displayName() : "Участник";
        String total = NotificationServiceImpl.formatRubles(event.totalKopecks());
        String message =
                payerName + " запустил сбор «" + event.eventTitle() + "» на " + total + " ₽. Выберите свои блюда";

        groupService.findById(event.groupId()).ifPresent(group -> {
            if (group.telegramChatId() != null) {
                notificationService.sendToGroupChat(
                        group.telegramChatId(), NotificationType.DISTRIBUTION_STARTED, message, event.eventId());
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDebtsCalculated(DebtsCalculated event) {
        long payerId = eventAccessPort.getPayerId(event.eventId());
        notificationService.send(
                payerId,
                NotificationType.DEBTS_CALCULATED,
                "Все выбрали блюда — проверь переводы");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDebtorConfirmed(DebtorConfirmed event) {
        String amount = NotificationServiceImpl.formatRubles(event.amountKopecks());
        notificationService.send(
                event.creditorId(),
                NotificationType.PAYMENT_PENDING,
                "Проверь перевод от " + event.debtorName() + " (" + amount + " ₽)");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentDisputed(PaymentDisputed event) {
        var payer = userService.findById(event.creditorId()).orElse(null);
        String payerName = payer != null ? payer.displayName() : "Плательщик";
        String amount = NotificationServiceImpl.formatRubles(event.amountKopecks());
        notificationService.send(
                event.debtorId(),
                NotificationType.PAYMENT_DISPUTED,
                payerName + " не видит твой перевод " + amount + " ₽ — проверь и отправь снова");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
