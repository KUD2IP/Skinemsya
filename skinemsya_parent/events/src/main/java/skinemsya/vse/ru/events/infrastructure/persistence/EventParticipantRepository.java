package skinemsya.vse.ru.events.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipantEntity, Long> {

    List<EventParticipantEntity> findByEventId(long eventId);
}
