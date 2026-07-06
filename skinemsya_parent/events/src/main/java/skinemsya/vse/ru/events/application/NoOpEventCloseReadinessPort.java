package skinemsya.vse.ru.events.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EventCloseReadinessPort.class)
public class NoOpEventCloseReadinessPort implements EventCloseReadinessPort {

    @Override
    public void assertReadyToClose(long eventId) {}
}
