package skinemsya.vse.ru.receipts.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;

@Service
@Transactional(readOnly = true)
public class PositionAvailabilityService {

    private final PositionSelectionRepository selectionRepository;

    public PositionAvailabilityService(PositionSelectionRepository selectionRepository) {
        this.selectionRepository = selectionRepository;
    }

    public PositionAvailability availabilityFor(PositionEntity position, long userId) {
        if (position.isShared()) {
            int totalUnits = toIntUnits(position.getQuantity());
            return new PositionAvailability(totalUnits, totalUnits, 0);
        }

        int totalUnits = toIntUnits(position.getQuantity());
        int mySelected = selectionRepository
                .findByPositionIdAndUserId(position.getId(), userId)
                .map(selection -> toIntUnits(selection.getSelectedQuantity()))
                .orElse(0);
        int selectedByOthers = toIntUnits(selectionRepository.sumSelectedByOthers(position.getId(), userId));
        int remaining = Math.max(0, totalUnits - selectedByOthers);
        boolean soldOut = remaining <= 0 && mySelected <= 0;
        return new PositionAvailability(totalUnits, remaining, mySelected, soldOut);
    }

    public void requireAvailableQuantity(PositionEntity position, long userId, BigDecimal requestedQuantity) {
        if (position.isShared()) {
            return;
        }
        var availability = availabilityFor(position, userId);
        int requested = toIntUnits(requestedQuantity);
        int maxAllowed = availability.remainingQuantity();
        if (requested > maxAllowed) {
            throw new skinemsya.vse.ru.receipts.domain.exception.SelectionExceedsAvailableQuantityException();
        }
    }

    static int toIntUnits(BigDecimal quantity) {
        if (quantity == null) {
            return 0;
        }
        return quantity.setScale(0, RoundingMode.DOWN).intValue();
    }

    public record PositionAvailability(
            int totalQuantity, int remainingQuantity, int mySelectedQuantity, boolean soldOut) {
        public PositionAvailability(int totalQuantity, int remainingQuantity, int mySelectedQuantity) {
            this(
                    totalQuantity,
                    remainingQuantity,
                    mySelectedQuantity,
                    remainingQuantity <= 0 && mySelectedQuantity <= 0);
        }
    }
}
