package skinemsya.vse.ru.receipts.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Position(
        long id,
        long eventId,
        Long receiptId,
        String name,
        BigDecimal quantity,
        long totalPriceKopecks,
        boolean shared,
        boolean tips,
        boolean lowConfidence,
        PositionSource source,
        Instant createdAt) {}
