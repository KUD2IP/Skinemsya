package skinemsya.vse.ru.impl.repository.events;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.data.entity.events.EventParticipantEntity;

/** Spring Data репозиторий участников сборов. */
public interface EventParticipantJpaRepository extends JpaRepository<EventParticipantEntity, Long> {

    List<EventParticipantEntity> findByEventId(long eventId);

    long countByEventId(long eventId);

    long countByEventIdAndSelectionCompletedAtIsNotNull(long eventId);

    Optional<EventParticipantEntity> findByEventIdAndUserId(long eventId, long userId);

    boolean existsByEventIdAndUserId(long eventId, long userId);

    void deleteByEventIdAndUserId(long eventId, long userId);
}
