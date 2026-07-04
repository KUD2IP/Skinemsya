package skinemsya.vse.ru.events.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipantEntity, Long> {

    List<EventParticipantEntity> findByEventId(long eventId);

    long countByEventId(long eventId);

    long countByEventIdAndSelectionCompletedAtIsNotNull(long eventId);

    Optional<EventParticipantEntity> findByEventIdAndUserId(long eventId, long userId);

    boolean existsByEventIdAndUserId(long eventId, long userId);
}
