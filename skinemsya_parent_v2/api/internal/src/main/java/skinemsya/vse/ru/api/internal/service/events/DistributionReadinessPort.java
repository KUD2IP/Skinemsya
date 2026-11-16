package skinemsya.vse.ru.api.internal.service.events;

/** Проверка, что у сбора есть позиции для распределения. */
public interface DistributionReadinessPort {

    /** Требует готовности сбора к распределению. */
    void assertReadyForDistribution(long eventId);

    /** Возвращает число позиций сбора. */
    long countPositions(long eventId);

    /** Возвращает сумму позиций сбора в копейках. */
    long sumTotalKopecks(long eventId);
}
