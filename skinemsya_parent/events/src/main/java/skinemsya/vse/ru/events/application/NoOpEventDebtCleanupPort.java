package skinemsya.vse.ru.events.application;

import java.util.Collection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

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
