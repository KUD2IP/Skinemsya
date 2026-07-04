package skinemsya.vse.ru.debts.domain;

import java.time.Instant;

public record Debt(
        long id,
        long eventId,
        long debtorId,
        long creditorId,
        long amountKopecks,
        DebtStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
