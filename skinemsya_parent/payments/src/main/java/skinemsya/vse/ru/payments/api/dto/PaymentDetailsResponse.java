package skinemsya.vse.ru.payments.api.dto;

import skinemsya.vse.ru.payments.domain.PaymentDetailsView;
import skinemsya.vse.ru.payments.domain.PaymentStatus;

public record PaymentDetailsResponse(
        long debtId,
        long eventId,
        long amountKopecks,
        long creditorId,
        String creditorName,
        String paymentDetails,
        String phone,
        String preferredBank,
        PaymentStatus status
) {
    public static PaymentDetailsResponse from(PaymentDetailsView view) {
        return new PaymentDetailsResponse(
                view.debtId(),
                view.eventId(),
                view.amountKopecks(),
                view.creditorId(),
                view.creditorName(),
                view.paymentDetails(),
                view.phone(),
                view.preferredBank(),
                view.status()
        );
    }
}
