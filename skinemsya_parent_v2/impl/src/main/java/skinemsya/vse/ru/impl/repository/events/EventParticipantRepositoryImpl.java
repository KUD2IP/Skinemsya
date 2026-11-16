package skinemsya.vse.ru.impl.repository.events;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.pojo.events.EventParticipant;
import skinemsya.vse.ru.api.internal.repository.events.EventParticipantRepository;
import skinemsya.vse.ru.impl.mapper.entity.EventEntityMapper;

/** Адаптер между внутренним портом участников сборов и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class EventParticipantRepositoryImpl implements EventParticipantRepository {

    private final EventParticipantJpaRepository eventParticipantJpaRepository;
    private final EventEntityMapper eventEntityMapper;

    @Override
    public List<EventParticipant> findByEventId(long eventId) {
        return eventParticipantJpaRepository.findByEventId(eventId).stream()
                .map(eventEntityMapper::toEventParticipant)
                .toList();
    }

    @Override
    public long countByEventId(long eventId) {
        return eventParticipantJpaRepository.countByEventId(eventId);
    }

    @Override
    public long countByEventIdAndSelectionCompletedAtIsNotNull(long eventId) {
        return eventParticipantJpaRepository.countByEventIdAndSelectionCompletedAtIsNotNull(eventId);
    }

    @Override
    public Optional<EventParticipant> findByEventIdAndUserId(long eventId, long userId) {
        return eventParticipantJpaRepository
                .findByEventIdAndUserId(eventId, userId)
                .map(eventEntityMapper::toEventParticipant);
    }

    @Override
    public boolean existsByEventIdAndUserId(long eventId, long userId) {
        return eventParticipantJpaRepository.existsByEventIdAndUserId(eventId, userId);
    }

    @Override
    public void deleteByEventIdAndUserId(long eventId, long userId) {
        eventParticipantJpaRepository.deleteByEventIdAndUserId(eventId, userId);
    }

    @Override
    public EventParticipant save(EventParticipant participant) {
        var entity = eventEntityMapper.toEntity(participant);
        if (participant.getId() == 0L) {
            entity.setId(null);
        }
        return eventEntityMapper.toEventParticipant(eventParticipantJpaRepository.save(entity));
    }
}
