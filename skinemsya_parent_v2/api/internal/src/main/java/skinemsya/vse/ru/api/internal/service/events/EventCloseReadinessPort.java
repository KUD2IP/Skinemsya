package skinemsya.vse.ru.api.internal.service.events;

/** Проверка, что сбор можно закрыть. */
public interface EventCloseReadinessPort {

    /** Требует готовности сбора к закрытию. */
    void assertReadyToClose(long eventId);
}
