package skinemsya.vse.ru.impl.service.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.service.events.DistributionReadinessPort;

/** Заглушка готовности к распределению до переноса чеков. */
@Component
@ConditionalOnMissingBean(DistributionReadinessPort.class)
public class NoOpDistributionReadinessPort implements DistributionReadinessPort {

    @Override
    public void assertReadyForDistribution(long eventId) {
        throw new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Event has no positions for distribution");
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
