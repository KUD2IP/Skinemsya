package skinemsya.vse.ru.events.application;

import java.util.Collection;

public interface EventDebtCleanupPort {

    void deleteUnpaidDebts(long eventId);

    void deleteUnpaidDebt(long eventId, long userId);

    boolean userHasUnconfirmedTransfer(long userId, Collection<Long> eventIds);
}
