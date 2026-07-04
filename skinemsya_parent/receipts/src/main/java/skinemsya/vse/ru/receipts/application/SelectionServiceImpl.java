package skinemsya.vse.ru.receipts.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.SelectionsCompleted;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventNotInDistributionException;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.receipts.domain.exception.PositionNotFoundException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;

import java.math.BigDecimal;
import java.util.List;

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
            PositionAvailabilityService positionAvailabilityService
    ) {
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

        for (var update : selections) {
            var position = positionRepository.findByIdAndEventId(update.positionId(), eventId)
                    .orElseThrow(PositionNotFoundException::new);
            if (update.quantity() == null || update.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                selectionRepository.findByPositionIdAndUserId(update.positionId(), userId)
                        .ifPresent(selectionRepository::delete);
                continue;
            }
            positionAvailabilityService.requireAvailableQuantity(position, userId, update.quantity());
            var selection = selectionRepository.findByPositionIdAndUserId(update.positionId(), userId)
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
        maybeCalculateDebts(eventId);
    }

    void maybeCalculateDebts(long eventId) {
        if (eventAccessPort.allSelectionsCompleted(eventId)) {
            eventPublisher.publishEvent(new SelectionsCompleted(eventId));
        }
    }

    private void requireDistribution(long eventId) {
        if (eventAccessPort.getStatus(eventId) != EventStatus.DISTRIBUTION) {
            throw new EventNotInDistributionException();
        }
    }
}
