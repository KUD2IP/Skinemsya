package skinemsya.vse.ru.receipts.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.debts.application.ReceiptDataPort;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.SharedPositionTargetRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReceiptDataPortImpl implements ReceiptDataPort {

    private final PositionRepository positionRepository;
    private final PositionSelectionRepository selectionRepository;
    private final SharedPositionTargetRepository sharedTargetRepository;

    public ReceiptDataPortImpl(
            PositionRepository positionRepository,
            PositionSelectionRepository selectionRepository,
            SharedPositionTargetRepository sharedTargetRepository
    ) {
        this.positionRepository = positionRepository;
        this.selectionRepository = selectionRepository;
        this.sharedTargetRepository = sharedTargetRepository;
    }

    @Override
    public List<PositionData> getPositions(long eventId) {
        return positionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(p -> new PositionData(p.getId(), p.getTotalPriceKopecks(), p.isShared()))
                .toList();
    }

    @Override
    public List<SelectionData> getSelections(long eventId) {
        var positionIds = positionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(p -> p.getId())
                .toList();
        if (positionIds.isEmpty()) {
            return List.of();
        }
        return selectionRepository.findByPositionIdIn(positionIds).stream()
                .map(s -> new SelectionData(s.getPositionId(), s.getUserId(), s.getSelectedQuantity()))
                .toList();
    }

    @Override
    public List<SharedTargetData> getSharedTargets(long eventId) {
        var positionIds = positionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(p -> p.getId())
                .toList();
        if (positionIds.isEmpty()) {
            return List.of();
        }
        return sharedTargetRepository.findByPositionIdIn(positionIds).stream()
                .map(t -> new SharedTargetData(t.getPositionId(), t.getUserId()))
                .toList();
    }
}
