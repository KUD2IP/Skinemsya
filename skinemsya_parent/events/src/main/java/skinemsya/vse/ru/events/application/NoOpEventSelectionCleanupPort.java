package skinemsya.vse.ru.events.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EventSelectionCleanupPort.class)
public class NoOpEventSelectionCleanupPort implements EventSelectionCleanupPort {

    @Override
    public void removeSelectionsForUser(long eventId, long userId) {}
}
