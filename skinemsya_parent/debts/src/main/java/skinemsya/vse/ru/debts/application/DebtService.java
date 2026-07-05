package skinemsya.vse.ru.debts.application;

import java.util.List;
import skinemsya.vse.ru.debts.domain.Debt;
import skinemsya.vse.ru.debts.domain.DebtSummary;

public interface DebtService {

    List<Debt> calculate(long eventId);

    void upsertDebtForParticipant(long eventId, long userId);

    void recalculateUnpaidDebts(long eventId);

    List<Debt> findByEvent(long eventId);

    DebtSummary getSummary(long userId);

    Debt markPendingConfirmation(long debtId);

    Debt close(long debtId);

    boolean allPaidForEvent(long eventId);
}
