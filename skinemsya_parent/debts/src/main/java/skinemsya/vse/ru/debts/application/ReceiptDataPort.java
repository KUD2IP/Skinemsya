package skinemsya.vse.ru.debts.application;

import java.math.BigDecimal;
import java.util.List;

public interface ReceiptDataPort {

    List<PositionData> getPositions(long eventId);

    List<SelectionData> getSelections(long eventId);

    List<SharedTargetData> getSharedTargets(long eventId);

    record PositionData(
            long id,
            long totalPriceKopecks,
            boolean shared
    ) {
    }

    record SelectionData(
            long positionId,
            long userId,
            BigDecimal selectedQuantity
    ) {
    }

    record SharedTargetData(
            long positionId,
            long userId
    ) {
    }
}
