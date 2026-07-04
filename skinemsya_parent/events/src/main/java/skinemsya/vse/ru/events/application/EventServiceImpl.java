package skinemsya.vse.ru.events.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventDeleteAccessRequiredException;
import skinemsya.vse.ru.events.domain.exception.EventNameRequiredException;
import skinemsya.vse.ru.events.domain.exception.EventNameTooLongException;
import skinemsya.vse.ru.events.domain.exception.EventNotDraftException;
import skinemsya.vse.ru.events.domain.exception.EventNotFoundException;
import skinemsya.vse.ru.events.domain.exception.EventUserNotFoundException;
import skinemsya.vse.ru.events.domain.exception.PayerNotGroupMemberException;
import skinemsya.vse.ru.events.infrastructure.mapper.EventMapper;
import skinemsya.vse.ru.events.infrastructure.persistence.EventEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;
import skinemsya.vse.ru.groups.application.GroupAccessService;
import skinemsya.vse.ru.groups.application.GroupDeletionGuard;
import skinemsya.vse.ru.groups.domain.exception.GroupHasBlockingEventsException;
import skinemsya.vse.ru.groups.domain.exception.GroupMemberAccessRequiredException;
import skinemsya.vse.ru.groups.domain.exception.GroupOwnerAccessRequiredException;
import skinemsya.vse.ru.users.application.UserService;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class EventServiceImpl implements EventService, GroupDeletionGuard {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventMapper eventMapper;
    private final GroupAccessService groupAccessService;
    private final UserService userService;

    public EventServiceImpl(
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            EventMapper eventMapper,
            GroupAccessService groupAccessService,
            UserService userService
    ) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventMapper = eventMapper;
        this.groupAccessService = groupAccessService;
        this.userService = userService;
    }

    @Override
    public Event create(long groupId, String name, String description, long payerId, long creatorId) {
        validateName(name);
        groupAccessService.requireMember(groupId, creatorId);
        requireUserExists(payerId);
        requireUserExists(creatorId);
        requirePayerIsGroupMember(groupId, payerId);

        var now = Instant.now();
        var entity = new EventEntity();
        entity.setGroupId(groupId);
        entity.setName(name.trim());
        entity.setDescription(normalizeDescription(description));
        entity.setPayerId(payerId);
        entity.setCreatedBy(creatorId);
        entity.setStatus(EventStatus.DRAFT);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity = eventRepository.save(entity);

        addParticipants(entity.getId(), groupId);
        return eventMapper.toDomain(entity);
    }

    @Override
    public Event update(long eventId, long requesterId, String name, String description, long payerId) {
        validateName(name);
        var entity = getActiveEvent(eventId);
        groupAccessService.requireMember(entity.getGroupId(), requesterId);
        requireDraft(entity);
        requirePayerIsGroupMember(entity.getGroupId(), payerId);
        requireUserExists(payerId);

        entity.setName(name.trim());
        entity.setDescription(normalizeDescription(description));
        entity.setPayerId(payerId);
        entity.setUpdatedAt(Instant.now());
        return eventMapper.toDomain(eventRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Event> findById(long eventId) {
        return eventRepository.findById(eventId).map(eventMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Event> listByGroup(long groupId, long requesterId, PageRequest pageRequest) {
        groupAccessService.requireMember(groupId, requesterId);
        var page = eventRepository.findByGroupIdOrderByCreatedAtDesc(
                groupId,
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size())
        );
        var items = page.getContent().stream().map(eventMapper::toDomain).toList();
        return PageResult.of(items, pageRequest, page.getTotalElements());
    }

    @Override
    public void delete(long eventId, long requesterId) {
        var entity = getActiveEvent(eventId);
        groupAccessService.requireMember(entity.getGroupId(), requesterId);
        requireDraft(entity);
        requireDeleteAccess(entity, requesterId);
        entity.setDeletedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        eventRepository.save(entity);
    }

    @Override
    public void ensureGroupCanBeDeleted(long groupId) {
        if (eventRepository.existsByGroupIdAndDeletedAtIsNullAndStatusNot(groupId, EventStatus.DRAFT)) {
            throw new GroupHasBlockingEventsException();
        }
    }

    @Override
    public void prepareGroupForDeletion(long groupId) {
        var now = Instant.now();
        var draftEvents = eventRepository.findByGroupIdAndStatusAndDeletedAtIsNull(groupId, EventStatus.DRAFT);
        for (var event : draftEvents) {
            event.setDeletedAt(now);
            event.setUpdatedAt(now);
            eventRepository.save(event);
        }
    }

    private void addParticipants(long eventId, long groupId) {
        Set<Long> userIds = new HashSet<>(groupAccessService.memberUserIds(groupId));
        for (long userId : userIds) {
            var participant = new EventParticipantEntity();
            participant.setEventId(eventId);
            participant.setUserId(userId);
            eventParticipantRepository.save(participant);
        }
    }

    private EventEntity getActiveEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);
    }

    private void requireDraft(EventEntity entity) {
        if (entity.getStatus() != EventStatus.DRAFT) {
            throw new EventNotDraftException();
        }
    }

    private void requireDeleteAccess(EventEntity entity, long requesterId) {
        if (entity.getCreatedBy() == requesterId) {
            return;
        }
        try {
            groupAccessService.requireOwner(entity.getGroupId(), requesterId);
        } catch (GroupMemberAccessRequiredException | GroupOwnerAccessRequiredException ex) {
            throw new EventDeleteAccessRequiredException();
        }
    }

    private void requirePayerIsGroupMember(long groupId, long payerId) {
        if (!groupAccessService.isMember(groupId, payerId)) {
            throw new PayerNotGroupMemberException();
        }
    }

    private void requireUserExists(long userId) {
        userService.findById(userId)
                .orElseThrow(EventUserNotFoundException::new);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new EventNameRequiredException();
        }
        if (name.trim().length() > 255) {
            throw new EventNameTooLongException();
        }
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
