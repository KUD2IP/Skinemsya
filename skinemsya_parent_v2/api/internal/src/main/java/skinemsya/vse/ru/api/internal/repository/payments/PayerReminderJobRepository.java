package skinemsya.vse.ru.api.internal.repository.payments;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.payments.PayerReminderJob;

/** Порт хранения напоминаний плательщику. */
public interface PayerReminderJobRepository {

    /** Возвращает неотправленные напоминания, срок которых уже наступил. */
    List<PayerReminderJob> findDue(Instant before);

    /** Находит напоминание по платежу. */
    Optional<PayerReminderJob> findByPaymentId(long paymentId);

    /** Сохраняет напоминание. */
    PayerReminderJob save(PayerReminderJob job);
}
