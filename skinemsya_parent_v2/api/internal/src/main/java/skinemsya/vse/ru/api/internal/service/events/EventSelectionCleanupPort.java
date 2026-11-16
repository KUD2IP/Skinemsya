package skinemsya.vse.ru.api.internal.service.events;

/** Очистка выбора позиций при изменении состава или статуса сбора. */
public interface EventSelectionCleanupPort {

    /** Удаляет выбор позиций пользователя в сборе. */
    void removeSelectionsForUser(long eventId, long userId);

    /** Удаляет весь выбор позиций сбора. */
    void removeSelectionsForEvent(long eventId);
}
