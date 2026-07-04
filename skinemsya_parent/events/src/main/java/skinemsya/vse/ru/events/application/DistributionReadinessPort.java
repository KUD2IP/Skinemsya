package skinemsya.vse.ru.events.application;

public interface DistributionReadinessPort {

    void assertReadyForDistribution(long eventId);

    long countPositions(long eventId);

    long sumTotalKopecks(long eventId);
}
