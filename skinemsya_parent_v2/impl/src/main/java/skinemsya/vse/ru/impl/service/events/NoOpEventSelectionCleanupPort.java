package skinemsya.vse.ru.impl.service.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.service.events.EventSelectionCleanupPort;

/** Заглушка очистки выбора позиций до переноса чеков. */
@Component
@ConditionalOnMissingBean(EventSelectionCleanupPort.class)
public class NoOpEventSelectionCleanupPort implements EventSelectionCleanupPort {

    @Override
    public void removeSelectionsForUser(long eventId, long userId) {}

    @Override
    public void removeSelectionsForEvent(long eventId) {}
}
