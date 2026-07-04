package skinemsya.vse.ru.receipts.domain;

import java.time.Instant;

public record Receipt(
        long id,
        long eventId,
        long fileId,
        ReceiptStatus status,
        Instant createdAt
) {
}
