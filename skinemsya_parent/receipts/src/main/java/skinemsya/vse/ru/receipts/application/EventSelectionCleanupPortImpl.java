package skinemsya.vse.ru.receipts.application;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.events.application.EventSelectionCleanupPort;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;

@Component
@Primary
public class EventSelectionCleanupPortImpl implements EventSelectionCleanupPort {

    private final PositionRepository positionRepository;
    private final PositionSelectionRepository selectionRepository;

    public EventSelectionCleanupPortImpl(
            PositionRepository positionRepository, PositionSelectionRepository selectionRepository) {
        this.positionRepository = positionRepository;
        this.selectionRepository = selectionRepository;
    }

    @Override
    @Transactional
    public void removeSelectionsForUser(long eventId, long userId) {
        var positionIds = positionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(position -> position.getId())
                .toList();
        if (positionIds.isEmpty()) {
            return;
        }
        selectionRepository.deleteByUserIdAndPositionIdIn(userId, positionIds);
    }
}
