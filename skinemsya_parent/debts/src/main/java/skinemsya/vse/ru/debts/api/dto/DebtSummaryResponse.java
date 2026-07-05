package skinemsya.vse.ru.debts.api.dto;

import skinemsya.vse.ru.debts.domain.DebtSummary;

public record DebtSummaryResponse(
        long totalOwedKopecks, long totalToReceiveKopecks, long unpaidCount, long pendingCount) {
    public static DebtSummaryResponse from(DebtSummary summary) {
        return new DebtSummaryResponse(
                summary.totalOwedKopecks(),
                summary.totalToReceiveKopecks(),
                summary.unpaidCount(),
                summary.pendingCount());
    }
}
