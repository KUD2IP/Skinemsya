package skinemsya.vse.ru.receipts.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.events.application.DistributionReadinessPort;
import skinemsya.vse.ru.events.domain.exception.NoPositionsException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;

@Service
@Transactional(readOnly = true)
public class DistributionReadinessPortImpl implements DistributionReadinessPort {

    private final PositionRepository positionRepository;

    public DistributionReadinessPortImpl(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Override
    public void assertReadyForDistribution(long eventId) {
        if (countPositions(eventId) < 1) {
            throw new NoPositionsException();
        }
    }

    @Override
    public long countPositions(long eventId) {
        return positionRepository.countByEventId(eventId);
    }

    @Override
    public long sumTotalKopecks(long eventId) {
        return positionRepository.sumTotalPriceKopecksByEventId(eventId);
    }
}
