package skinemsya.vse.ru.api.internal.repository.events;

import java.util.List;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.events.EventParticipant;

/** Порт хранения участников сборов. */
public interface EventParticipantRepository {

    /** Возвращает участников сбора. */
    List<EventParticipant> findByEventId(long eventId);

    /** Возвращает число участников сбора. */
    long countByEventId(long eventId);

    /** Возвращает число участников, закончивших выбор позиций. */
    long countByEventIdAndSelectionCompletedAtIsNotNull(long eventId);

    /** Находит участника сбора. */
    Optional<EventParticipant> findByEventIdAndUserId(long eventId, long userId);

    /** Проверяет, состоит ли пользователь в сборе. */
    boolean existsByEventIdAndUserId(long eventId, long userId);

    /** Удаляет участника сбора. */
    void deleteByEventIdAndUserId(long eventId, long userId);

    /** Сохраняет участника сбора. */
    EventParticipant save(EventParticipant participant);
}
