package skinemsya.vse.ru.receipts.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import skinemsya.vse.ru.receipts.application.PositionAvailabilityService;
import skinemsya.vse.ru.receipts.domain.Position;
import skinemsya.vse.ru.receipts.domain.PositionSource;

public record PositionResponse(
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
        Instant createdAt,
        Integer remainingQuantity,
        Integer mySelectedQuantity,
        Boolean soldOut) {
    public static PositionResponse from(Position position) {
        return new PositionResponse(
                position.id(),
                position.eventId(),
                position.receiptId(),
                position.name(),
                position.quantity(),
                position.totalPriceKopecks(),
                position.shared(),
                position.tips(),
                position.lowConfidence(),
                position.source(),
                position.createdAt(),
                null,
                null,
                null);
    }

    public static PositionResponse from(
            Position position, PositionAvailabilityService.PositionAvailability availability) {
        return new PositionResponse(
                position.id(),
                position.eventId(),
                position.receiptId(),
                position.name(),
                position.quantity(),
                position.totalPriceKopecks(),
                position.shared(),
                position.tips(),
                position.lowConfidence(),
                position.source(),
                position.createdAt(),
                availability.remainingQuantity(),
                availability.mySelectedQuantity(),
                availability.soldOut());
    }
}
