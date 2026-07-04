package skinemsya.vse.ru.events.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.events.domain.exception.NoPositionsException;

@Component
@ConditionalOnMissingBean(DistributionReadinessPort.class)
public class NoOpDistributionReadinessPort implements DistributionReadinessPort {

    @Override
    public void assertReadyForDistribution(long eventId) {
        throw new NoPositionsException();
    }

    @Override
    public long countPositions(long eventId) {
        return 0;
    }

    @Override
    public long sumTotalKopecks(long eventId) {
        return 0;
    }
}
