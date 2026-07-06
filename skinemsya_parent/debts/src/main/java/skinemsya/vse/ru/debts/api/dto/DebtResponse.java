package skinemsya.vse.ru.debts.api.dto;

import java.time.Instant;
import skinemsya.vse.ru.debts.application.PaymentInfo;
import skinemsya.vse.ru.debts.domain.Debt;
import skinemsya.vse.ru.debts.domain.DebtStatus;

public record DebtResponse(
        long id,
        long eventId,
        long debtorId,
        long creditorId,
        long amountKopecks,
        DebtStatus status,
        String paymentStatus,
        Long screenshotFileId,
        Instant createdAt,
        Instant updatedAt) {
    public static DebtResponse from(Debt debt) {
        return from(debt, null);
    }

    public static DebtResponse from(Debt debt, PaymentInfo paymentInfo) {
        return new DebtResponse(
                debt.id(),
                debt.eventId(),
                debt.debtorId(),
                debt.creditorId(),
                debt.amountKopecks(),
                debt.status(),
                paymentInfo != null ? paymentInfo.status() : null,
                paymentInfo != null ? paymentInfo.screenshotFileId() : null,
                debt.createdAt(),
                debt.updatedAt());
    }
}
