package skinemsya.vse.ru.payments.application;

import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.DebtorConfirmed;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.payments.infrastructure.persistence.PayerReminderJobRepository;
import skinemsya.vse.ru.payments.infrastructure.persistence.PaymentRepository;
import skinemsya.vse.ru.users.application.UserService;

@Component
public class PayerReminderJobScheduler {

    private final PayerReminderJobRepository reminderJobRepository;
    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public PayerReminderJobScheduler(
            PayerReminderJobRepository reminderJobRepository,
            PaymentRepository paymentRepository,
            DebtRepository debtRepository,
            UserService userService,
            ApplicationEventPublisher eventPublisher) {
        this.reminderJobRepository = reminderJobRepository;
        this.paymentRepository = paymentRepository;
        this.debtRepository = debtRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processDueReminders() {
        var dueJobs = reminderJobRepository.findBySentAtIsNullAndScheduledAtBefore(Instant.now());
        for (var job : dueJobs) {
            paymentRepository.findById(job.getPaymentId()).ifPresent(payment -> {
                debtRepository.findById(payment.getDebtId()).ifPresent(debt -> {
                    var debtor = userService.findById(debt.getDebtorId()).orElse(null);
                    if (debtor != null) {
                        eventPublisher.publishEvent(new DebtorConfirmed(
                                debt.getEventId(),
                                payment.getId(),
                                debt.getDebtorId(),
                                debt.getCreditorId(),
                                debt.getAmountKopecks(),
                                debtor.displayName()));
                    }
                });
            });
            job.setSentAt(Instant.now());
            reminderJobRepository.save(job);
        }
    }
}
