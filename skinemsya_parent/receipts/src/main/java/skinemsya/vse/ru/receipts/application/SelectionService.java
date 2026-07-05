package skinemsya.vse.ru.receipts.application;

import java.math.BigDecimal;
import java.util.List;

public interface SelectionService {

    void updateSelections(long eventId, long userId, List<SelectionUpdate> selections);

    void completeSelection(long eventId, long userId);

    record SelectionUpdate(long positionId, BigDecimal quantity) {}
}
