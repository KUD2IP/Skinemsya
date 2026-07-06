package skinemsya.vse.ru.payments.application;

import java.util.List;
import skinemsya.vse.ru.payments.domain.Payment;
import skinemsya.vse.ru.payments.domain.PaymentDetailsView;

public interface PaymentService {

    PaymentDetailsView getPaymentDetails(long debtId, long requesterId);

    Payment confirmByDebtor(long debtId, long debtorId, Long screenshotFileId);

    List<Payment> confirmAllForEvent(long eventId, long payerId);

    Payment confirmByPayer(long paymentId, long payerId);

    Payment confirmByPayerForDebt(long debtId, long payerId);

    Payment dispute(long paymentId, long payerId);

    Payment disputeForDebt(long debtId, long payerId);
}
