package skinemsya.vse.ru.payments.api.dto;

import skinemsya.vse.ru.payments.domain.Payment;
import skinemsya.vse.ru.payments.domain.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(
        long id,
        long debtId,
        PaymentStatus status,
        Long screenshotFileId,
        Instant debtorConfirmedAt,
        Instant payerConfirmedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.debtId(),
                payment.status(),
                payment.screenshotFileId(),
                payment.debtorConfirmedAt(),
                payment.payerConfirmedAt(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
