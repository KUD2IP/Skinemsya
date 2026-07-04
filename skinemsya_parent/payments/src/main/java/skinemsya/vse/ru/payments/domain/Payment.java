package skinemsya.vse.ru.payments.domain;

import java.time.Instant;

public record Payment(
        long id,
        long debtId,
        PaymentStatus status,
        Long screenshotFileId,
        Instant debtorConfirmedAt,
        Instant payerConfirmedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
