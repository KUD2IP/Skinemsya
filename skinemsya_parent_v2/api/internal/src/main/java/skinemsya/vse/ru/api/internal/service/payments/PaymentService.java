package skinemsya.vse.ru.api.internal.service.payments;

import java.util.List;
import skinemsya.vse.ru.api.internal.pojo.payments.Payment;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentDetailsView;

/** Подтверждение и оспаривание переводов по долгам. */
public interface PaymentService {

    /** Возвращает реквизиты кредитора для оплаты долга. */
    PaymentDetailsView getPaymentDetails(long debtId, long requesterId);

    /** Должник отмечает, что перевод отправлен. */
    Payment confirmByDebtor(long debtId, long debtorId, Long screenshotFileId);

    /** Плательщик подтверждает все отмеченные переводы сбора. */
    List<Payment> confirmAllForEvent(long eventId, long payerId);

    /** Плательщик подтверждает перевод по идентификатору платежа. */
    Payment confirmByPayer(long paymentId, long payerId);

    /** Плательщик подтверждает перевод по идентификатору долга. */
    Payment confirmByPayerForDebt(long debtId, long payerId);

    /** Плательщик оспаривает перевод по идентификатору платежа. */
    Payment dispute(long paymentId, long payerId);

    /** Плательщик оспаривает перевод по идентификатору долга. */
    Payment disputeForDebt(long debtId, long payerId);
}
