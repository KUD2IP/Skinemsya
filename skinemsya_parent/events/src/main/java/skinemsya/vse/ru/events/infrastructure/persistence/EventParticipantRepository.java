package skinemsya.vse.ru.events.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventParticipantRepository extends JpaRepository<EventParticipantEntity, Long> {

    List<EventParticipantEntity> findByEventId(long eventId);

    long countByEventId(long eventId);

    long countByEventIdAndSelectionCompletedAtIsNotNull(long eventId);

    Optional<EventParticipantEntity> findByEventIdAndUserId(long eventId, long userId);

    boolean existsByEventIdAndUserId(long eventId, long userId);
}
