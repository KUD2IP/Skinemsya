package skinemsya.vse.ru.debts.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnMissingBean(ReceiptDataPort.class)
public class NoOpReceiptDataPort implements ReceiptDataPort {

    @Override
    public List<PositionData> getPositions(long eventId) {
        return Collections.emptyList();
    }

    @Override
    public List<SelectionData> getSelections(long eventId) {
        return Collections.emptyList();
    }

    @Override
    public List<SharedTargetData> getSharedTargets(long eventId) {
        return Collections.emptyList();
    }
}
