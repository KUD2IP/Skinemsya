package skinemsya.vse.ru.receipts.domain;

import java.time.Instant;

public record Receipt(
        long id,
        long eventId,
        long fileId,
        ReceiptStatus status,
        Instant createdAt,
        Long totalKopecks,
        Long itemsSumKopecks,
        boolean sumMismatch) {

    public static boolean sumMismatch(Long totalKopecks, Long itemsSumKopecks) {
        if (totalKopecks == null || itemsSumKopecks == null) {
            return false;
        }
        long tolerance = Math.max(1L, Math.round(Math.abs(totalKopecks) * 0.01));
        return Math.abs(totalKopecks - itemsSumKopecks) > tolerance;
    }
}
