package skinemsya.vse.ru.impl.service.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.service.events.EventCloseReadinessPort;

/** Заглушка готовности к закрытию сбора до переноса платежей. */
@Component
@ConditionalOnMissingBean(EventCloseReadinessPort.class)
public class NoOpEventCloseReadinessPort implements EventCloseReadinessPort {

    @Override
    public void assertReadyToClose(long eventId) {}
}
