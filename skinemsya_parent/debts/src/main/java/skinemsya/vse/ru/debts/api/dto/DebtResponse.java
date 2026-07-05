package skinemsya.vse.ru.debts.api.dto;

import java.time.Instant;
import skinemsya.vse.ru.debts.domain.Debt;
import skinemsya.vse.ru.debts.domain.DebtStatus;

public record DebtResponse(
        long id,
        long eventId,
        long debtorId,
        long creditorId,
        long amountKopecks,
        DebtStatus status,
        Instant createdAt,
        Instant updatedAt) {
    public static DebtResponse from(Debt debt) {
        return new DebtResponse(
                debt.id(),
                debt.eventId(),
                debt.debtorId(),
                debt.creditorId(),
                debt.amountKopecks(),
                debt.status(),
                debt.createdAt(),
                debt.updatedAt());
    }
}
