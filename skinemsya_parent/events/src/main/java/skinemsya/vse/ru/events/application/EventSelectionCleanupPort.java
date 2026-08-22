package skinemsya.vse.ru.events.application;

public interface EventSelectionCleanupPort {

    void removeSelectionsForUser(long eventId, long userId);
}
