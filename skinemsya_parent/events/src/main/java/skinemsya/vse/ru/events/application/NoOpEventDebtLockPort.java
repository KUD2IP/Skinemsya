package skinemsya.vse.ru.events.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EventDebtLockPort.class)
public class NoOpEventDebtLockPort implements EventDebtLockPort {

    @Override
    public boolean hasLockedDebts(long eventId) {
        return false;
    }
}
