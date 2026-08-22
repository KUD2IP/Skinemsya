package skinemsya.vse.ru.payments.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PayerReminderJobScheduler {

    @SuppressWarnings("unused")
    private final ApplicationEventPublisher eventPublisher;

    public PayerReminderJobScheduler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 60_000)
    public void processDueReminders() {
        // Group-chat notifications replace the 2-hour payer DM. Existing payer_reminder_jobs stay unused.
    }
}
