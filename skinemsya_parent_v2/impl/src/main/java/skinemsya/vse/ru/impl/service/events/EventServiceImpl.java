package skinemsya.vse.ru.impl.service.events;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.event.events.EventCompleted;
import skinemsya.vse.ru.api.internal.event.events.EventParticipantsChanged;
import skinemsya.vse.ru.api.internal.event.events.EventSentToDistribution;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.events.Event;
import skinemsya.vse.ru.api.internal.pojo.events.EventParticipant;
import skinemsya.vse.ru.api.internal.pojo.users.PayoutRequisites;
import skinemsya.vse.ru.api.internal.repository.events.EventParticipantRepository;
import skinemsya.vse.ru.api.internal.repository.events.EventRepository;
import skinemsya.vse.ru.api.internal.service.events.DistributionReadinessPort;
import skinemsya.vse.ru.api.internal.service.events.EventAccessPort;
import skinemsya.vse.ru.api.internal.service.events.EventCloseReadinessPort;
import skinemsya.vse.ru.api.internal.service.events.EventDebtCleanupPort;
import skinemsya.vse.ru.api.internal.service.events.EventDebtLockPort;
import skinemsya.vse.ru.api.internal.service.events.EventSelectionCleanupPort;
import skinemsya.vse.ru.api.internal.service.events.EventService;
import skinemsya.vse.ru.api.internal.service.groups.GroupAccessService;
import skinemsya.vse.ru.api.internal.service.groups.GroupDeletionGuard;
import skinemsya.vse.ru.api.internal.service.groups.GroupMemberEventCleanup;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.impl.support.util.InviteCodes;

/** Реализация операций со сборами поверх внутренних портов. */
@Service
@Transactional
@RequiredArgsConstructor
public class EventServiceImpl implements EventService, EventAccessPort, GroupDeletionGuard, GroupMemberEventCleanup {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final GroupAccessService groupAccessService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributionReadinessPort distributionReadinessPort;
    private final EventCloseReadinessPort eventCloseReadinessPort;
    private final EventDebtLockPort eventDebtLockPort;
    private final EventSelectionCleanupPort eventSelectionCleanupPort;
    private final EventDebtCleanupPort eventDebtCleanupPort;

    @Override
    public Event create(
            long groupId, String name, String description, long payerId, long creatorId, int expectedParticipantCount) {
        validateName(name);
        groupAccessService.requireMember(groupId, creatorId);
        requireUserExists(payerId);
        requireUserExists(creatorId);
        requirePayerIsGroupMember(groupId, payerId);

        var now = Instant.now();
        var event = Event.builder()
                .groupId(groupId)
                .name(name.trim())
                .description(normalizeDescription(description))
                .payerId(payerId)
                .createdBy(creatorId)
                .inviteCode(InviteCodes.random())
                .status(EventStatus.DRAFT)
                .expectedParticipantCount(expectedParticipantCount)
                .createdAt(now)
                .updatedAt(now)
                .build();
        event = eventRepository.save(event);

        addSeedParticipants(event.getId(), creatorId, payerId);
        int joined = (int) eventParticipantRepository.countByEventId(event.getId());
        validateExpectedCount(expectedParticipantCount, joined);
        return event;
    }

    @Override
    public Event update(
            long eventId,
            long requesterId,
            String name,
            String description,
            long payerId,
            int expectedParticipantCount) {
        validateName(name);
        var event = getActiveEvent(eventId);
        groupAccessService.requireMember(event.getGroupId(), requesterId);
        requireDraft(event);
        requirePayerIsGroupMember(event.getGroupId(), payerId);
        requireUserExists(payerId);
        addParticipant(eventId, payerId);
        validateExpectedCount(expectedParticipantCount, eventParticipantRepository.countByEventId(eventId));

        event.setName(name.trim());
        event.setDescription(normalizeDescription(description));
        event.setPayerId(payerId);
        event.setExpectedParticipantCount(expectedParticipantCount);
        event.setUpdatedAt(Instant.now());
        return eventRepository.save(event);
    }

    @Override
    public Event join(long eventId, long userId) {
        var event = getActiveEvent(eventId);
        groupAccessService.requireMember(event.getGroupId(), userId);
        if (eventParticipantRepository.existsByEventIdAndUserId(eventId, userId)) {
            return event;
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw conflict("Cannot join this event");
        }
        if (eventParticipantRepository.countByEventId(eventId) >= event.getExpectedParticipantCount()) {
            throw conflict("Event has no free seats");
        }
        addParticipant(eventId, userId);
        eventPublisher.publishEvent(new EventParticipantsChanged(eventId, event.getGroupId()));
        return event;
    }

    @Override
    public Event leave(long eventId, long userId) {
        var event = getActiveEvent(eventId);
        requireParticipant(eventId, userId);
        if (event.getPayerId() == userId || event.getCreatedBy() == userId) {
            throw conflict("Cannot leave this event");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw conflict("Cannot leave this event");
        }
        if (eventDebtCleanupPort.userHasUnconfirmedTransfer(userId, List.of(eventId))) {
            throw conflict("Нельзя выйти: вы уже отметили перевод, а плательщик его ещё не подтвердил");
        }
        removeParticipantRecord(eventId, userId, event.getGroupId());
        return event;
    }

    @Override
    public Event revertToDraft(long eventId, long requesterId) {
        var event = getActiveEvent(eventId);
        if (event.getPayerId() != requesterId && event.getCreatedBy() != requesterId) {
            throw denied("Only payer or creator can return the event to draft");
        }
        if (event.getStatus() == EventStatus.DRAFT) {
            return event;
        }
        if (event.getStatus() != EventStatus.DISTRIBUTION && event.getStatus() != EventStatus.CALCULATED) {
            throw rule("Event can be returned to draft only before payments are confirmed");
        }
        if (eventDebtLockPort.hasLockedDebts(eventId)) {
            throw rule("Event can be returned to draft only before payments are confirmed");
        }

        eventSelectionCleanupPort.removeSelectionsForEvent(eventId);
        clearAllSelectionCompleted(eventId);
        eventDebtCleanupPort.deleteUnpaidDebts(eventId);

        event.setStatus(EventStatus.DRAFT);
        event.setUpdatedAt(Instant.now());
        return eventRepository.save(event);
    }

    @Override
    public Event removeParticipant(long eventId, long requesterId, long targetUserId) {
        var event = getActiveEvent(eventId);
        requireOwnerOrThrow(event.getGroupId(), requesterId, conflict("Cannot remove this participant from the event"));
        requireParticipant(eventId, targetUserId);
        if (event.getPayerId() == targetUserId || event.getCreatedBy() == targetUserId) {
            throw conflict("Cannot remove this participant from the event");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw conflict("Cannot remove this participant from the event");
        }
        if (eventDebtLockPort.hasLockedDebts(eventId)) {
            throw conflict("Cannot remove this participant from the event");
        }
        removeParticipantRecord(eventId, targetUserId, event.getGroupId());
        return event;
    }

    @Override
    public Event updateExpectedParticipantCount(long eventId, long requesterId, int expectedParticipantCount) {
        var event = getActiveEvent(eventId);
        groupAccessService.requireMember(event.getGroupId(), requesterId);
        if (event.getPayerId() != requesterId && event.getCreatedBy() != requesterId) {
            throw conflict("Expected participant count cannot be changed");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw conflict("Expected participant count cannot be changed");
        }
        if (event.getStatus() != EventStatus.DRAFT && eventDebtLockPort.hasLockedDebts(eventId)) {
            throw conflict("Expected participant count cannot be changed");
        }
        validateExpectedCount(expectedParticipantCount, eventParticipantRepository.countByEventId(eventId));
        event.setExpectedParticipantCount(expectedParticipantCount);
        event.setUpdatedAt(Instant.now());
        event = eventRepository.save(event);
        eventPublisher.publishEvent(new EventParticipantsChanged(eventId, event.getGroupId()));
        return event;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Event> findById(long eventId) {
        return eventRepository.findById(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Event> listByGroup(long groupId, long requesterId, PageRequest pageRequest) {
        groupAccessService.requireMember(groupId, requesterId);
        return eventRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageRequest);
    }

    @Override
    public void delete(long eventId, long requesterId) {
        var event = getActiveEvent(eventId);
        groupAccessService.requireMember(event.getGroupId(), requesterId);
        requireDeleteAccess(event, requesterId);
        var now = Instant.now();
        eventRepository.markDeleted(eventId, now, now);
    }

    @Override
    @Transactional(readOnly = true)
    public long getEventGroupId(long eventId) {
        return getActiveEvent(eventId).getGroupId();
    }

    @Override
    @Transactional(readOnly = true)
    public String inviteCode(long eventId) {
        return getActiveEvent(eventId).getInviteCode();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findIdByInviteCode(String inviteCode) {
        if (!InviteCodes.isToken(inviteCode)) {
            return Optional.empty();
        }
        return eventRepository.findByInviteCode(inviteCode).map(Event::getId);
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
            throw denied("User is not an event participant");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countParticipants(long eventId) {
        return eventParticipantRepository.countByEventId(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getExpectedParticipantCount(long eventId) {
        return getActiveEvent(eventId).getExpectedParticipantCount();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isParticipant(long eventId, long userId) {
        return eventParticipantRepository.existsByEventIdAndUserId(eventId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getParticipantUserIds(long eventId) {
        return eventParticipantRepository.findByEventId(eventId).stream()
                .map(EventParticipant::getUserId)
                .sorted()
                .toList();
    }

    @Override
    public void markCalculated(long eventId) {
        var event = getActiveEvent(eventId);
        if (event.getStatus() != EventStatus.DISTRIBUTION) {
            throw conflict("Event is not in distribution status");
        }
        event.setStatus(EventStatus.CALCULATED);
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);
    }

    @Override
    public Event closeByPayer(long eventId, long payerId) {
        var event = getActiveEvent(eventId);
        if (event.getPayerId() != payerId) {
            throw denied("Only payer can close the event");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            return event;
        }
        if (event.getStatus() != EventStatus.CALCULATED) {
            throw conflict("Event debts are not calculated yet");
        }
        eventCloseReadinessPort.assertReadyToClose(eventId);
        event.setStatus(EventStatus.COMPLETED);
        event.setUpdatedAt(Instant.now());
        event = eventRepository.save(event);
        eventPublisher.publishEvent(new EventCompleted(eventId, event.getGroupId(), event.getName()));
        return event;
    }

    @Override
    public Event sendToDistribution(long eventId, long requesterId) {
        var event = getActiveEvent(eventId);
        requireDraft(event);
        if (event.getPayerId() != requesterId && event.getCreatedBy() != requesterId) {
            throw denied("Only payer or creator can send event to distribution");
        }
        distributionReadinessPort.assertReadyForDistribution(eventId);
        var paymentDetails = userService.getPaymentDetails(event.getPayerId());
        if (!PayoutRequisites.hasAny(paymentDetails)) {
            throw rule("Payer payment details are required before distribution");
        }

        event.setStatus(EventStatus.DISTRIBUTION);
        event.setUpdatedAt(Instant.now());
        event = eventRepository.save(event);

        long totalKopecks = distributionReadinessPort.sumTotalKopecks(eventId);
        eventPublisher.publishEvent(new EventSentToDistribution(
                eventId, event.getGroupId(), event.getPayerId(), event.getName(), totalKopecks));
        return event;
    }

    @Override
    public void ensureGroupCanBeDeleted(long groupId) {
        // Группу можно удалить независимо от статуса сборов.
    }

    @Override
    public void prepareGroupForDeletion(long groupId) {
        var now = Instant.now();
        for (var event : eventRepository.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            eventRepository.markDeleted(event.getId(), now, now);
        }
    }

    @Override
    public void assertUserIsNotPayerOfActiveEvents(long groupId, long userId) {
        if (eventRepository.existsByGroupIdAndPayerId(groupId, userId)) {
            throw rule("Нельзя выйти из группы: вы плательщик в незакрытом сборе");
        }
    }

    @Override
    public void assertUserHasNoUnconfirmedTransfer(long groupId, long userId) {
        var eventIds = eventRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(Event::getId)
                .toList();
        if (eventIds.isEmpty()) {
            return;
        }
        if (eventDebtCleanupPort.userHasUnconfirmedTransfer(userId, eventIds)) {
            throw rule("Нельзя выйти: вы уже отметили перевод, а плательщик его ещё не подтвердил");
        }
    }

    @Override
    public void removeUserFromGroupEvents(long groupId, long userId) {
        for (var event : eventRepository.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            if (!eventParticipantRepository.existsByEventIdAndUserId(event.getId(), userId)) {
                continue;
            }
            removeParticipantRecord(event.getId(), userId, groupId);
        }
    }

    @Override
    public void markSelectionCompleted(long eventId, long userId) {
        requireParticipant(eventId, userId);
        var participant = eventParticipantRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> denied("User is not an event participant"));
        participant.setSelectionCompletedAt(Instant.now());
        eventParticipantRepository.save(participant);
    }

    @Override
    public void clearSelectionCompleted(long eventId, long userId) {
        requireParticipant(eventId, userId);
        var participant = eventParticipantRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> denied("User is not an event participant"));
        participant.setSelectionCompletedAt(null);
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
                .map(EventParticipant::getUserId)
                .sorted()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getIncompleteSelectionParticipantUserIds(long eventId) {
        return eventParticipantRepository.findByEventId(eventId).stream()
                .filter(participant -> participant.getSelectionCompletedAt() == null)
                .map(EventParticipant::getUserId)
                .sorted()
                .toList();
    }

    @Override
    public void revertCalculatedToDistribution(long eventId) {
        var event = getActiveEvent(eventId);
        if (event.getStatus() != EventStatus.CALCULATED) {
            return;
        }
        event.setStatus(EventStatus.DISTRIBUTION);
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);
    }

    private void addSeedParticipants(long eventId, long creatorId, long payerId) {
        addParticipant(eventId, creatorId);
        if (payerId != creatorId) {
            addParticipant(eventId, payerId);
        }
    }

    private void addParticipant(long eventId, long userId) {
        if (eventParticipantRepository.existsByEventIdAndUserId(eventId, userId)) {
            return;
        }
        eventParticipantRepository.save(EventParticipant.builder()
                .eventId(eventId)
                .userId(userId)
                .build());
    }

    private void clearAllSelectionCompleted(long eventId) {
        for (var participant : eventParticipantRepository.findByEventId(eventId)) {
            if (participant.getSelectionCompletedAt() == null) {
                continue;
            }
            participant.setSelectionCompletedAt(null);
            eventParticipantRepository.save(participant);
        }
    }

    private void removeParticipantRecord(long eventId, long userId, long groupId) {
        eventSelectionCleanupPort.removeSelectionsForUser(eventId, userId);
        eventDebtCleanupPort.deleteUnpaidDebt(eventId, userId);
        eventParticipantRepository.deleteByEventIdAndUserId(eventId, userId);
        eventPublisher.publishEvent(new EventParticipantsChanged(eventId, groupId));
    }

    private static void validateExpectedCount(int expectedParticipantCount, long joinedCount) {
        if (expectedParticipantCount < 2 || expectedParticipantCount > 99) {
            throw invalid("Expected participant count is invalid");
        }
        if (expectedParticipantCount < joinedCount) {
            throw invalid("Expected participant count is invalid");
        }
    }

    private Event getActiveEvent(long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> notFound("Event not found"));
    }

    private void requireDraft(Event event) {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw rule("Event can only be modified in DRAFT status");
        }
    }

    private void requireDeleteAccess(Event event, long requesterId) {
        if (event.getCreatedBy() == requesterId || event.getPayerId() == requesterId) {
            return;
        }
        requireOwnerOrThrow(event.getGroupId(), requesterId, denied("Event delete access required"));
    }

    private void requireOwnerOrThrow(long groupId, long userId, DomainException denied) {
        try {
            groupAccessService.requireOwner(groupId, userId);
        } catch (DomainException ex) {
            if (ex.errorCode() == ErrorCode.AUTHORIZATION_ERROR) {
                throw denied;
            }
            throw ex;
        }
    }

    private void requirePayerIsGroupMember(long groupId, long payerId) {
        if (!groupAccessService.isMember(groupId, payerId)) {
            throw rule("Payer must be a group member");
        }
    }

    private void requireUserExists(long userId) {
        userService.findById(userId).orElseThrow(() -> notFound("User not found"));
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw invalid("Event name is required");
        }
        if (name.trim().length() > 255) {
            throw invalid("Event name is too long");
        }
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private static DomainException conflict(String message) {
        return new DomainException(ErrorCode.DOMAIN_CONFLICT, message);
    }

    private static DomainException rule(String message) {
        return new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, message);
    }

    private static DomainException denied(String message) {
        return new DomainException(ErrorCode.AUTHORIZATION_ERROR, message);
    }

    private static DomainException notFound(String message) {
        return new DomainException(ErrorCode.NOT_FOUND, message);
    }

    private static DomainException invalid(String message) {
        return new DomainException(ErrorCode.VALIDATION_ERROR, message);
    }
}
