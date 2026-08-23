package skinemsya.vse.ru.receipts.application;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.SelectionsCompleted;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventNotInDistributionException;
import skinemsya.vse.ru.receipts.domain.exception.PositionNotFoundException;
import skinemsya.vse.ru.receipts.domain.exception.SelectionCannotBeReopenedException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;

@Service
@Transactional
public class SelectionServiceImpl implements SelectionService {

    private final PositionRepository positionRepository;
    private final PositionSelectionRepository selectionRepository;
    private final EventAccessPort eventAccessPort;
    private final ApplicationEventPublisher eventPublisher;
    private final DebtService debtService;
    private final PositionAvailabilityService positionAvailabilityService;

    public SelectionServiceImpl(
            PositionRepository positionRepository,
            PositionSelectionRepository selectionRepository,
            EventAccessPort eventAccessPort,
            ApplicationEventPublisher eventPublisher,
            DebtService debtService,
            PositionAvailabilityService positionAvailabilityService) {
        this.positionRepository = positionRepository;
        this.selectionRepository = selectionRepository;
        this.eventAccessPort = eventAccessPort;
        this.eventPublisher = eventPublisher;
        this.debtService = debtService;
        this.positionAvailabilityService = positionAvailabilityService;
    }

    @Override
    public void updateSelections(long eventId, long userId, List<SelectionUpdate> selections) {
        requireDistribution(eventId);
        eventAccessPort.requireParticipant(eventId, userId);

        var incoming = selections == null ? List.<SelectionUpdate>of() : selections;
        var incomingIds = incoming.stream().map(SelectionUpdate::positionId).toList();
        var stalePositionIds = positionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(PositionEntity::getId)
                .filter(positionId -> !incomingIds.contains(positionId))
                .toList();
        if (!stalePositionIds.isEmpty()) {
            selectionRepository.deleteByUserIdAndPositionIdIn(userId, stalePositionIds);
        }

        for (var update : incoming) {
            var position = positionRepository
                    .findByIdAndEventId(update.positionId(), eventId)
                    .orElseThrow(PositionNotFoundException::new);
            if (update.quantity() == null || update.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                selectionRepository
                        .findByPositionIdAndUserId(update.positionId(), userId)
                        .ifPresent(selectionRepository::delete);
                continue;
            }
            positionAvailabilityService.requireAvailableQuantity(position, userId, update.quantity());
            var selection = selectionRepository
                    .findByPositionIdAndUserId(update.positionId(), userId)
                    .orElseGet(() -> {
                        var entity = new PositionSelectionEntity();
                        entity.setPositionId(update.positionId());
                        entity.setUserId(userId);
                        return entity;
                    });
            selection.setSelectedQuantity(update.quantity());
            selectionRepository.save(selection);
        }
    }

    @Override
    public void completeSelection(long eventId, long userId) {
        requireDistribution(eventId);
        eventAccessPort.markSelectionCompleted(eventId, userId);
        debtService.upsertDebtForParticipant(eventId, userId);
        autoCompleteEmptySelections(eventId);
        maybeCalculateDebts(eventId);
    }

    @Override
    public void reopenSelection(long eventId, long userId) {
        eventAccessPort.requireParticipant(eventId, userId);
        var status = eventAccessPort.getStatus(eventId);
        if (status != EventStatus.DISTRIBUTION && status != EventStatus.CALCULATED) {
            throw new SelectionCannotBeReopenedException();
        }
        boolean hasLockedDebt =
                debtService.findByEvent(eventId).stream().anyMatch(debt -> debt.status() != DebtStatus.UNPAID);
        if (hasLockedDebt) {
            throw new SelectionCannotBeReopenedException();
        }
        eventAccessPort.clearSelectionCompleted(eventId, userId);
        debtService.recalculateUnpaidDebts(eventId);
    }

    void maybeCalculateDebts(long eventId) {
        if (eventAccessPort.allSelectionsCompleted(eventId)) {
            eventPublisher.publishEvent(new SelectionsCompleted(eventId));
        }
    }

    void autoCompleteEmptySelections(long eventId) {
        var positions = positionRepository.findByEventIdOrderByCreatedAtAsc(eventId);
        for (long participantId : eventAccessPort.getIncompleteSelectionParticipantUserIds(eventId)) {
            if (!hasNothingToSelect(positions, participantId)) {
                continue;
            }
            eventAccessPort.markSelectionCompleted(eventId, participantId);
            debtService.upsertDebtForParticipant(eventId, participantId);
        }
    }

    private boolean hasNothingToSelect(List<PositionEntity> positions, long userId) {
        boolean hasShared = false;
        boolean canSelect = false;
        boolean hasOwnSelection = false;
        for (var position : positions) {
            if (position.isShared()) {
                hasShared = true;
                continue;
            }
            var availability = positionAvailabilityService.availabilityFor(position, userId);
            if (availability.remainingQuantity() > 0) {
                canSelect = true;
            }
            if (availability.mySelectedQuantity() > 0) {
                hasOwnSelection = true;
            }
        }
        return !hasShared && !canSelect && !hasOwnSelection;
    }

    private void requireDistribution(long eventId) {
        if (eventAccessPort.getStatus(eventId) != EventStatus.DISTRIBUTION) {
            throw new EventNotInDistributionException();
        }
    }
}
