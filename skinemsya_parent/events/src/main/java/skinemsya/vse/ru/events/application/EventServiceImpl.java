package skinemsya.vse.ru.events.application;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.common.event.EventCompleted;
import skinemsya.vse.ru.common.event.EventSentToDistribution;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventCloseAccessRequiredException;
import skinemsya.vse.ru.events.domain.exception.EventDeleteAccessRequiredException;
import skinemsya.vse.ru.events.domain.exception.EventDistributionAccessRequiredException;
import skinemsya.vse.ru.events.domain.exception.EventNameRequiredException;
import skinemsya.vse.ru.events.domain.exception.EventNameTooLongException;
import skinemsya.vse.ru.events.domain.exception.EventNotCalculatedException;
import skinemsya.vse.ru.events.domain.exception.EventNotDraftException;
import skinemsya.vse.ru.events.domain.exception.EventNotFoundException;
import skinemsya.vse.ru.events.domain.exception.EventNotInDistributionException;
import skinemsya.vse.ru.events.domain.exception.EventNotParticipantException;
import skinemsya.vse.ru.events.domain.exception.EventUserNotFoundException;
import skinemsya.vse.ru.events.domain.exception.PayerNotGroupMemberException;
import skinemsya.vse.ru.events.domain.exception.PaymentDetailsMissingException;
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
import skinemsya.vse.ru.users.domain.PayoutRequisites;

@Service
@Transactional
public class EventServiceImpl implements EventService, EventAccessPort, GroupDeletionGuard {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventMapper eventMapper;
    private final GroupAccessService groupAccessService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributionReadinessPort distributionReadinessPort;
    private final EventParticipantSyncService eventParticipantSyncService;
    private final EventCloseReadinessPort eventCloseReadinessPort;

    public EventServiceImpl(
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            EventMapper eventMapper,
            GroupAccessService groupAccessService,
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            DistributionReadinessPort distributionReadinessPort,
            EventParticipantSyncService eventParticipantSyncService,
            EventCloseReadinessPort eventCloseReadinessPort) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventMapper = eventMapper;
        this.groupAccessService = groupAccessService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.distributionReadinessPort = distributionReadinessPort;
        this.eventParticipantSyncService = eventParticipantSyncService;
        this.eventCloseReadinessPort = eventCloseReadinessPort;
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
                groupId, org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size()));
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
    @Transactional(readOnly = true)
    public long getEventGroupId(long eventId) {
        return getActiveEvent(eventId).getGroupId();
    }

    @Override
    @Transactional(readOnly = true)
    public long getPayerId(long eventId) {
        return getActiveEvent(eventId).getPayerId();
    }

    @Override
    @Transactional(readOnly = true)
    public EventStatus getStatus(long eventId) {
        return getActiveEvent(eventId).getStatus();
    }

    @Override
    public void requireParticipant(long eventId, long userId) {
        if (!eventParticipantRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new EventNotParticipantException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countParticipants(long eventId) {
        return eventParticipantRepository.countByEventId(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getParticipantUserIds(long eventId) {
        return eventParticipantRepository.findByEventId(eventId).stream()
                .map(EventParticipantEntity::getUserId)
                .sorted()
                .toList();
    }

    @Override
    public void markCalculated(long eventId) {
        var entity = getActiveEvent(eventId);
        if (entity.getStatus() != EventStatus.DISTRIBUTION) {
            throw new EventNotInDistributionException();
        }
        entity.setStatus(EventStatus.CALCULATED);
        entity.setUpdatedAt(Instant.now());
        eventRepository.save(entity);
    }

    @Override
    public Event closeByPayer(long eventId, long payerId) {
        var entity = getActiveEvent(eventId);
        if (entity.getPayerId() != payerId) {
            throw new EventCloseAccessRequiredException();
        }
        if (entity.getStatus() == EventStatus.COMPLETED) {
            return eventMapper.toDomain(entity);
        }
        if (entity.getStatus() != EventStatus.CALCULATED) {
            throw new EventNotCalculatedException();
        }
        eventCloseReadinessPort.assertReadyToClose(eventId);
        entity.setStatus(EventStatus.COMPLETED);
        entity.setUpdatedAt(Instant.now());
        entity = eventRepository.save(entity);
        eventPublisher.publishEvent(new EventCompleted(eventId, entity.getGroupId(), entity.getName()));
        return eventMapper.toDomain(entity);
    }

    @Override
    public Event sendToDistribution(long eventId, long requesterId) {
        var entity = getActiveEvent(eventId);
        requireDraft(entity);
        if (entity.getPayerId() != requesterId && entity.getCreatedBy() != requesterId) {
            throw new EventDistributionAccessRequiredException();
        }
        eventParticipantSyncService.syncAllMembersBeforeDistribution(eventId, entity.getGroupId());
        distributionReadinessPort.assertReadyForDistribution(eventId);
        var paymentDetails = userService.getPaymentDetails(entity.getPayerId());
        if (!PayoutRequisites.hasAny(paymentDetails)) {
            throw new PaymentDetailsMissingException();
        }

        entity.setStatus(EventStatus.DISTRIBUTION);
        entity.setUpdatedAt(Instant.now());
        entity = eventRepository.save(entity);

        long totalKopecks = distributionReadinessPort.sumTotalKopecks(eventId);
        eventPublisher.publishEvent(new EventSentToDistribution(
                eventId, entity.getGroupId(), entity.getPayerId(), entity.getName(), totalKopecks));
        return eventMapper.toDomain(entity);
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

    @Override
    public void markSelectionCompleted(long eventId, long userId) {
        requireParticipant(eventId, userId);
        var participant = eventParticipantRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(EventNotParticipantException::new);
        participant.setSelectionCompletedAt(Instant.now());
        eventParticipantRepository.save(participant);
    }

    @Override
    public boolean allSelectionsCompleted(long eventId) {
        long total = eventParticipantRepository.countByEventId(eventId);
        long completed = eventParticipantRepository.countByEventIdAndSelectionCompletedAtIsNotNull(eventId);
        return total > 0 && total == completed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getSelectionCompletedParticipantUserIds(long eventId) {
        return eventParticipantRepository.findByEventId(eventId).stream()
                .filter(participant -> participant.getSelectionCompletedAt() != null)
                .map(EventParticipantEntity::getUserId)
                .sorted()
                .toList();
    }

    @Override
    public void revertCalculatedToDistribution(long eventId) {
        var entity = getActiveEvent(eventId);
        if (entity.getStatus() != EventStatus.CALCULATED) {
            return;
        }
        entity.setStatus(EventStatus.DISTRIBUTION);
        entity.setUpdatedAt(Instant.now());
        eventRepository.save(entity);
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
        return eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
    }

    private void requireDraft(EventEntity entity) {
        if (entity.getStatus() != EventStatus.DRAFT) {
            throw new EventNotDraftException();
        }
    }

    void requireDistribution(EventEntity entity) {
        if (entity.getStatus() != EventStatus.DISTRIBUTION) {
            throw new EventNotInDistributionException();
        }
    }

    void requireDraftForEdit(EventEntity entity) {
        requireDraft(entity);
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
        userService.findById(userId).orElseThrow(EventUserNotFoundException::new);
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
