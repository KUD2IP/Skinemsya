package skinemsya.vse.ru.impl.service.payments;

import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Планировщик напоминаний плательщику. Личные сообщения заменены уведомлениями в чат группы. */
@Component
public class PayerReminderJobScheduler {

    private final ApplicationEventPublisher eventPublisher;

    public PayerReminderJobScheduler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** Не отправляет напоминания: очередь payer_reminder_jobs больше не используется. */
    @Scheduled(fixedRate = 60_000)
    public void processDueReminders() {
        Objects.requireNonNull(eventPublisher);
    }
}
