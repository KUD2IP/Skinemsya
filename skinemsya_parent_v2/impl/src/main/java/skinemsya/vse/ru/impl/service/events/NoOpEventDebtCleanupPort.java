package skinemsya.vse.ru.impl.service.events;

import java.util.Collection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.service.events.EventDebtCleanupPort;

/** Заглушка очистки долгов до переноса модуля долгов. */
@Component
@ConditionalOnMissingBean(EventDebtCleanupPort.class)
public class NoOpEventDebtCleanupPort implements EventDebtCleanupPort {

    @Override
    public void deleteUnpaidDebts(long eventId) {}

    @Override
    public void deleteUnpaidDebt(long eventId, long userId) {}

    @Override
    public boolean userHasUnconfirmedTransfer(long userId, Collection<Long> eventIds) {
        return false;
    }
}
