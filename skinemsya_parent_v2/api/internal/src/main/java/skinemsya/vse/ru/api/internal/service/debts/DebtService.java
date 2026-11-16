package skinemsya.vse.ru.api.internal.service.debts;

import java.util.List;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.pojo.debts.DebtSummary;

/** Расчёт и жизненный цикл долгов сбора. */
public interface DebtService {

    /** Рассчитывает долги сбора и переводит его в статус рассчитанного. */
    List<Debt> calculate(long eventId);

    /** Создаёт или обновляет неоплаченный долг участника. */
    void upsertDebtForParticipant(long eventId, long userId);

    /** Пересчитывает неоплаченные долги, не трогая уже отмеченные переводы. */
    void recalculateUnpaidDebts(long eventId);

    /** Возвращает долги сбора. */
    List<Debt> findByEvent(long eventId);

    /** Возвращает сводку долгов пользователя. */
    DebtSummary getSummary(long userId);

    /** Переводит долг в ожидание подтверждения плательщиком. */
    Debt markPendingConfirmation(long debtId);

    /** Закрывает долг как оплаченный. */
    Debt close(long debtId);

    /** Проверяет, что все долги сбора оплачены. */
    boolean allPaidForEvent(long eventId);
}
