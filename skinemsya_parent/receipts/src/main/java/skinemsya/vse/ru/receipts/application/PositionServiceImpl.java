package skinemsya.vse.ru.receipts.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventNotDraftException;
import skinemsya.vse.ru.receipts.domain.Position;
import skinemsya.vse.ru.receipts.domain.PositionSource;
import skinemsya.vse.ru.receipts.domain.exception.PositionNotFoundException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.SharedPositionTargetRepository;

@Service
@Transactional
public class PositionServiceImpl implements PositionService {

    private final PositionRepository positionRepository;
    private final PositionSelectionRepository selectionRepository;
    private final SharedPositionTargetRepository sharedTargetRepository;
    private final EventAccessPort eventAccessPort;

    public PositionServiceImpl(
            PositionRepository positionRepository,
            PositionSelectionRepository selectionRepository,
            SharedPositionTargetRepository sharedTargetRepository,
            EventAccessPort eventAccessPort) {
        this.positionRepository = positionRepository;
        this.selectionRepository = selectionRepository;
        this.sharedTargetRepository = sharedTargetRepository;
        this.eventAccessPort = eventAccessPort;
    }

    @Override
    public Position addManual(long eventId, long userId, String name, BigDecimal quantity, long totalPriceKopecks) {
        requireDraft(eventId);
        eventAccessPort.requireParticipant(eventId, userId);
        validatePositionInput(name, quantity, totalPriceKopecks);

        var entity = new PositionEntity();
        entity.setEventId(eventId);
        entity.setName(name.trim());
        entity.setQuantity(quantity);
        entity.setTotalPriceKopecks(totalPriceKopecks);
        entity.setSource(PositionSource.MANUAL);
        entity.setCreatedAt(Instant.now());
        return toDomain(positionRepository.save(entity));
    }

    @Override
    public Position update(long positionId, long userId, String name, BigDecimal quantity, long totalPriceKopecks) {
        var entity = getPosition(positionId);
        requireDraft(entity.getEventId());
        eventAccessPort.requireParticipant(entity.getEventId(), userId);
        validatePositionInput(name, quantity, totalPriceKopecks);

        entity.setName(name.trim());
        entity.setQuantity(quantity);
        entity.setTotalPriceKopecks(totalPriceKopecks);
        return toDomain(positionRepository.save(entity));
    }

    @Override
    public void delete(long positionId, long userId) {
        var entity = getPosition(positionId);
        requireDraft(entity.getEventId());
        eventAccessPort.requireParticipant(entity.getEventId(), userId);
        selectionRepository.deleteByPositionId(positionId);
        sharedTargetRepository.deleteByPositionId(positionId);
        positionRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Position> listByEvent(long eventId, long userId) {
        eventAccessPort.requireParticipant(eventId, userId);
        return positionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Position markShared(long positionId, long userId, boolean forAll, List<Long> targetUserIds) {
        var entity = getPosition(positionId);
        requireDraft(entity.getEventId());
        eventAccessPort.requireParticipant(entity.getEventId(), userId);

        entity.setShared(true);
        sharedTargetRepository.deleteByPositionId(positionId);
        if (!forAll && targetUserIds != null) {
            for (long targetUserId : targetUserIds) {
                eventAccessPort.requireParticipant(entity.getEventId(), targetUserId);
                var target = new skinemsya.vse.ru.receipts.infrastructure.persistence.SharedPositionTargetEntity();
                target.setPositionId(positionId);
                target.setUserId(targetUserId);
                sharedTargetRepository.save(target);
            }
        }
        return toDomain(positionRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByEvent(long eventId) {
        return positionRepository.countByEventId(eventId);
    }

    Position toDomain(PositionEntity entity) {
        return new Position(
                entity.getId(),
                entity.getEventId(),
                entity.getReceiptId(),
                entity.getName(),
                entity.getQuantity(),
                entity.getTotalPriceKopecks(),
                entity.isShared(),
                entity.isTips(),
                entity.isLowConfidence(),
                entity.getSource(),
                entity.getCreatedAt());
    }

    PositionEntity getPosition(long positionId) {
        return positionRepository.findById(positionId).orElseThrow(PositionNotFoundException::new);
    }

    private void requireDraft(long eventId) {
        if (eventAccessPort.getStatus(eventId) != EventStatus.DRAFT) {
            throw new EventNotDraftException();
        }
    }

    private static void validatePositionInput(String name, BigDecimal quantity, long totalPriceKopecks) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Position name is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            throw new IllegalArgumentException("Position quantity must be at least 0.01");
        }
        if (totalPriceKopecks < 0) {
            throw new IllegalArgumentException("Position price cannot be negative");
        }
    }
}
