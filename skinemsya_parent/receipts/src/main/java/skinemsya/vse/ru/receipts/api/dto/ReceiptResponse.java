package skinemsya.vse.ru.receipts.api.dto;

import skinemsya.vse.ru.receipts.domain.Receipt;
import skinemsya.vse.ru.receipts.domain.ReceiptStatus;

import java.time.Instant;

public record ReceiptResponse(
        long id,
        long eventId,
        long fileId,
        ReceiptStatus status,
        Instant createdAt
) {
    public static ReceiptResponse from(Receipt receipt) {
        return new ReceiptResponse(
                receipt.id(),
                receipt.eventId(),
                receipt.fileId(),
                receipt.status(),
                receipt.createdAt()
        );
    }
}
