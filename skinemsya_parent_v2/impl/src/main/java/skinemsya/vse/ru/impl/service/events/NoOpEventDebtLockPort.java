package skinemsya.vse.ru.impl.service.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.service.events.EventDebtLockPort;

/** Заглушка блокировки долгов до переноса модуля долгов. */
@Component
@ConditionalOnMissingBean(EventDebtLockPort.class)
public class NoOpEventDebtLockPort implements EventDebtLockPort {

    @Override
    public boolean hasLockedDebts(long eventId) {
        return false;
    }
}
