package skinemsya.vse.ru.impl.service.receipts;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.pojo.receipts.PositionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SelectionData;
import skinemsya.vse.ru.api.internal.pojo.receipts.SharedTargetData;
import skinemsya.vse.ru.api.internal.service.receipts.ReceiptDataPort;

/** Пустые данные чека до переноса модуля чеков. */
@Component
@ConditionalOnMissingBean(ReceiptDataPort.class)
public class NoOpReceiptDataPort implements ReceiptDataPort {

    @Override
    public List<PositionData> getPositions(long eventId) {
        return List.of();
    }

    @Override
    public List<SelectionData> getSelections(long eventId) {
        return List.of();
    }

    @Override
    public List<SharedTargetData> getSharedTargets(long eventId) {
        return List.of();
    }
}
