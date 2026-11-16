package skinemsya.vse.ru.api.internal.repository.payments;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;
import skinemsya.vse.ru.api.internal.pojo.payments.Payment;

/** Порт хранения платежей. */
public interface PaymentRepository {

    /** Находит платёж по идентификатору. */
    Optional<Payment> findById(long paymentId);

    /** Находит платёж по долгу. */
    Optional<Payment> findByDebtId(long debtId);

    /** Возвращает платежи по списку долгов. */
    List<Payment> findByDebtIdIn(Collection<Long> debtIds);

    /** Находит платёж по файлу скриншота. */
    Optional<Payment> findByScreenshotFileId(long screenshotFileId);

    /** Возвращает платежи в заданном статусе. */
    List<Payment> findByStatus(PaymentStatus status);

    /** Сохраняет платёж. */
    Payment save(Payment payment);
}
