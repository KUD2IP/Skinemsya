package skinemsya.vse.ru.api.internal.service.events;

import java.util.Collection;

/** Очистка неоплаченных долгов сбора. */
public interface EventDebtCleanupPort {

    /** Удаляет неоплаченные долги сбора. */
    void deleteUnpaidDebts(long eventId);

    /** Удаляет неоплаченный долг участника. */
    void deleteUnpaidDebt(long eventId, long userId);

    /** Проверяет неподтверждённый перевод пользователя по сборам. */
    boolean userHasUnconfirmedTransfer(long userId, Collection<Long> eventIds);
}
