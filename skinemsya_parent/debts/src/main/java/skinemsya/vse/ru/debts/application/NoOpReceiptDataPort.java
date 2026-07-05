package skinemsya.vse.ru.debts.application;

import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

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
