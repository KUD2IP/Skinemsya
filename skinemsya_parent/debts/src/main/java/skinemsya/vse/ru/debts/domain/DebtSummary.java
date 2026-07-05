package skinemsya.vse.ru.debts.domain;

public record DebtSummary(long totalOwedKopecks, long totalToReceiveKopecks, long unpaidCount, long pendingCount) {}
