package skinemsya.vse.ru.impl.repository.events;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.events.Event;
import skinemsya.vse.ru.api.internal.repository.events.EventRepository;
import skinemsya.vse.ru.data.entity.events.EventEntity;
import skinemsya.vse.ru.impl.mapper.entity.EventEntityMapper;

/** Адаптер между внутренним портом сборов и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepository {

    private final EventJpaRepository eventJpaRepository;
    private final EventEntityMapper eventEntityMapper;

    @Override
    public Optional<Event> findById(long eventId) {
        return eventJpaRepository.findById(eventId).map(eventEntityMapper::toEvent);
    }

    @Override
    public Optional<Event> findByInviteCode(String inviteCode) {
        return eventJpaRepository.findByInviteCode(inviteCode).map(eventEntityMapper::toEvent);
    }

    @Override
    public List<Event> findByGroupIdOrderByCreatedAtDesc(long groupId) {
        return eventJpaRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(eventEntityMapper::toEvent)
                .toList();
    }

    @Override
    public PageResult<Event> findByGroupIdOrderByCreatedAtDesc(long groupId, PageRequest pageRequest) {
        Page<EventEntity> page = eventJpaRepository.findByGroupIdOrderByCreatedAtDesc(
                groupId, org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size()));
        return new PageResult<>(
                page.getContent().stream().map(eventEntityMapper::toEvent).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Override
    public boolean existsByGroupId(long groupId) {
        return eventJpaRepository.existsByGroupIdAndDeletedAtIsNull(groupId);
    }

    @Override
    public boolean existsByGroupIdAndStatusNot(long groupId, EventStatus status) {
        return eventJpaRepository.existsByGroupIdAndDeletedAtIsNullAndStatusNot(groupId, status);
    }

    @Override
    public boolean existsByGroupIdAndPayerId(long groupId, long payerId) {
        return eventJpaRepository.existsByGroupIdAndPayerId(groupId, payerId);
    }

    @Override
    public List<Event> findByGroupIdAndStatus(long groupId, EventStatus status) {
        return eventJpaRepository.findByGroupIdAndStatusAndDeletedAtIsNull(groupId, status).stream()
                .map(eventEntityMapper::toEvent)
                .toList();
    }

    @Override
    public List<Event> findByGroupIdAndStatusIn(long groupId, Collection<EventStatus> statuses) {
        return eventJpaRepository.findByGroupIdAndDeletedAtIsNullAndStatusIn(groupId, statuses).stream()
                .map(eventEntityMapper::toEvent)
                .toList();
    }

    @Override
    public Event save(Event event) {
        var entity = eventEntityMapper.toEntity(event);
        if (event.getId() == 0L) {
            entity.setId(null);
        }
        return eventEntityMapper.toEvent(eventJpaRepository.save(entity));
    }

    @Override
    public void markDeleted(long eventId, Instant deletedAt, Instant updatedAt) {
        eventJpaRepository.markDeleted(eventId, deletedAt, updatedAt);
    }
}
