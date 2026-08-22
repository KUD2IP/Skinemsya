package skinemsya.vse.ru.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventCannotLeaveException;
import skinemsya.vse.ru.events.domain.exception.EventCannotRemoveParticipantException;
import skinemsya.vse.ru.events.domain.exception.EventDeleteAccessRequiredException;
import skinemsya.vse.ru.events.domain.exception.EventFullException;
import skinemsya.vse.ru.events.domain.exception.PayerNotGroupMemberException;
import skinemsya.vse.ru.groups.domain.exception.GroupMemberIsEventPayerException;
import skinemsya.vse.ru.groups.domain.exception.GroupOwnerAccessRequiredException;
import skinemsya.vse.ru.events.infrastructure.mapper.EventMapper;
import skinemsya.vse.ru.events.infrastructure.persistence.EventEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;
import skinemsya.vse.ru.groups.application.GroupAccessService;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.User;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private GroupAccessService groupAccessService;

    @Mock
    private UserService userService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private DistributionReadinessPort distributionReadinessPort;

    @Mock
    private EventCloseReadinessPort eventCloseReadinessPort;

    @Mock
    private EventDebtLockPort eventDebtLockPort;

    @Mock
    private EventSelectionCleanupPort eventSelectionCleanupPort;

    @InjectMocks
    private EventServiceImpl eventService;

    private static final long GROUP_ID = 10L;
    private static final long CREATOR_ID = 1L;
    private static final long PAYER_ID = 1L;
    private static final long OTHER_MEMBER_ID = 2L;

    @Test
    void shouldCreateDraftEventWithCreatorAndPayerOnly() {
        var saved = eventEntity(100L, EventStatus.DRAFT);
        var domain = domainEvent(100L, EventStatus.DRAFT);

        when(userService.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID)));
        when(userService.findById(PAYER_ID)).thenReturn(Optional.of(user(PAYER_ID)));
        when(groupAccessService.isMember(GROUP_ID, PAYER_ID)).thenReturn(true);
        when(eventRepository.save(any(EventEntity.class))).thenReturn(saved);
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, CREATOR_ID)).thenReturn(false);
        when(eventParticipantRepository.countByEventId(100L)).thenReturn(1L);
        when(eventMapper.toDomain(saved)).thenReturn(domain);

        var result = eventService.create(GROUP_ID, "Dinner", "Desc", PAYER_ID, CREATOR_ID, 4);

        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(result.payerId()).isEqualTo(PAYER_ID);
        verify(groupAccessService).requireMember(GROUP_ID, CREATOR_ID);
        verify(eventParticipantRepository, times(1)).save(any(EventParticipantEntity.class));
    }

    @Test
    void shouldRejectCreateWhenPayerNotGroupMember() {
        when(userService.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID)));
        when(userService.findById(OTHER_MEMBER_ID)).thenReturn(Optional.of(user(OTHER_MEMBER_ID)));
        when(groupAccessService.isMember(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> eventService.create(GROUP_ID, "Dinner", null, OTHER_MEMBER_ID, CREATOR_ID, 4))
                .isInstanceOf(PayerNotGroupMemberException.class)
                .extracting(ex -> ((PayerNotGroupMemberException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    @Test
    void shouldUpdatePayerInDraftEvent() {
        var existing = eventEntity(100L, EventStatus.DRAFT);
        var updated = domainEvent(100L, EventStatus.DRAFT);

        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(groupAccessService.isMember(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(true);
        when(userService.findById(OTHER_MEMBER_ID)).thenReturn(Optional.of(user(OTHER_MEMBER_ID)));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(false);
        when(eventParticipantRepository.countByEventId(100L)).thenReturn(2L);
        when(eventRepository.save(existing)).thenReturn(existing);
        when(eventMapper.toDomain(existing)).thenReturn(updated);

        eventService.update(100L, CREATOR_ID, "Dinner", "New desc", OTHER_MEMBER_ID, 4);

        assertThat(existing.getPayerId()).isEqualTo(OTHER_MEMBER_ID);
        assertThat(existing.getExpectedParticipantCount()).isEqualTo(4);
    }

    @Test
    void shouldRejectJoinWhenEventIsFull() {
        var existing = eventEntity(100L, EventStatus.DISTRIBUTION);
        existing.setExpectedParticipantCount(2);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(false);
        when(eventParticipantRepository.countByEventId(100L)).thenReturn(2L);

        assertThatThrownBy(() -> eventService.join(100L, OTHER_MEMBER_ID)).isInstanceOf(EventFullException.class);
    }

    @Test
    void shouldDeleteEventInAnyStatusWhenRequesterIsPayer() {
        var existing = eventEntity(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.delete(100L, PAYER_ID);

        assertThat(existing.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldDeleteEventWhenRequesterIsGroupOwner() {
        var existing = eventEntity(100L, EventStatus.COMPLETED);
        long ownerId = 9L;
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.delete(100L, ownerId);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(groupAccessService).requireOwner(GROUP_ID, ownerId);
    }

    @Test
    void shouldRejectDeleteForRegularMember() {
        var existing = eventEntity(100L, EventStatus.DRAFT);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        doThrow(new GroupOwnerAccessRequiredException()).when(groupAccessService).requireOwner(GROUP_ID, OTHER_MEMBER_ID);

        assertThatThrownBy(() -> eventService.delete(100L, OTHER_MEMBER_ID))
                .isInstanceOf(EventDeleteAccessRequiredException.class);
        assertThat(existing.getDeletedAt()).isNull();
    }

    @Test
    void shouldSoftDeleteAllEventsWhenPreparingGroupDeletion() {
        var draft = eventEntity(1L, EventStatus.DRAFT);
        var live = eventEntity(2L, EventStatus.DISTRIBUTION);
        when(eventRepository.findByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of(draft, live));

        eventService.prepareGroupForDeletion(GROUP_ID);

        assertThat(draft.getDeletedAt()).isNotNull();
        assertThat(live.getDeletedAt()).isNotNull();
        verify(eventRepository).save(draft);
        verify(eventRepository).save(live);
    }

    @Test
    void shouldRejectGroupMemberCleanupWhenUserIsPayer() {
        when(eventRepository.existsByGroupIdAndPayerId(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.assertUserIsNotPayerOfActiveEvents(GROUP_ID, OTHER_MEMBER_ID))
                .isInstanceOf(GroupMemberIsEventPayerException.class);
    }

    @Test
    void shouldRemoveRegularParticipantByGroupOwner() {
        var existing = eventEntity(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(true);
        when(eventMapper.toDomain(existing)).thenReturn(domainEvent(100L, EventStatus.DISTRIBUTION));

        eventService.removeParticipant(100L, CREATOR_ID, OTHER_MEMBER_ID);

        verify(eventSelectionCleanupPort).removeSelectionsForUser(100L, OTHER_MEMBER_ID);
        verify(eventParticipantRepository).deleteByEventIdAndUserId(100L, OTHER_MEMBER_ID);
    }

    @Test
    void shouldRejectRemovePayerFromEvent() {
        var existing = eventEntity(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, PAYER_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.removeParticipant(100L, CREATOR_ID, PAYER_ID))
                .isInstanceOf(EventCannotRemoveParticipantException.class);
    }

    @Test
    void shouldRejectLeaveForPayer() {
        var existing = eventEntity(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, PAYER_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.leave(100L, PAYER_ID)).isInstanceOf(EventCannotLeaveException.class);
    }

    private static User user(long id) {
        return new User(id, 100_000L + id, "User " + id, null, Instant.now(), Instant.now());
    }

    private static EventEntity eventEntity(long id, EventStatus status) {
        var entity = new EventEntity();
        entity.setId(id);
        entity.setGroupId(GROUP_ID);
        entity.setName("Dinner");
        entity.setPayerId(PAYER_ID);
        entity.setCreatedBy(CREATOR_ID);
        entity.setStatus(status);
        entity.setExpectedParticipantCount(4);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static Event domainEvent(long id, EventStatus status) {
        return new Event(id, GROUP_ID, "Dinner", "Desc", PAYER_ID, CREATOR_ID, status, 4, Instant.now(), Instant.now());
    }
}
