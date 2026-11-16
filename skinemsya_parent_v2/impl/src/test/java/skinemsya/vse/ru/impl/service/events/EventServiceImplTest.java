package skinemsya.vse.ru.impl.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUser;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.events.Event;
import skinemsya.vse.ru.api.internal.pojo.events.EventParticipant;
import skinemsya.vse.ru.api.internal.repository.events.EventParticipantRepository;
import skinemsya.vse.ru.api.internal.repository.events.EventRepository;
import skinemsya.vse.ru.api.internal.service.events.DistributionReadinessPort;
import skinemsya.vse.ru.api.internal.service.events.EventCloseReadinessPort;
import skinemsya.vse.ru.api.internal.service.events.EventDebtCleanupPort;
import skinemsya.vse.ru.api.internal.service.events.EventDebtLockPort;
import skinemsya.vse.ru.api.internal.service.events.EventSelectionCleanupPort;
import skinemsya.vse.ru.api.internal.service.groups.GroupAccessService;
import skinemsya.vse.ru.api.internal.service.users.UserService;

/** Тесты операций со сборами. */
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final long GROUP_ID = 10L;
    private static final long CREATOR_ID = 1L;
    private static final long PAYER_ID = 1L;
    private static final long OTHER_MEMBER_ID = 2L;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private GroupAccessService groupAccessService;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DistributionReadinessPort distributionReadinessPort;

    @Mock
    private EventCloseReadinessPort eventCloseReadinessPort;

    @Mock
    private EventDebtLockPort eventDebtLockPort;

    @Mock
    private EventSelectionCleanupPort eventSelectionCleanupPort;

    @Mock
    private EventDebtCleanupPort eventDebtCleanupPort;

    @InjectMocks
    private EventServiceImpl eventService;

    /** Создаёт черновик и добавляет только создателя, если он же плательщик. */
    @Test
    void shouldCreateDraftEventWithCreatorAndPayerOnly() {
        when(userService.findById(CREATOR_ID)).thenReturn(Optional.of(createUser()));
        when(groupAccessService.isMember(GROUP_ID, PAYER_ID)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(100L);
            return event;
        });
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, CREATOR_ID)).thenReturn(false);
        when(eventParticipantRepository.countByEventId(100L)).thenReturn(1L);

        var result = eventService.create(GROUP_ID, "Dinner", "Desc", PAYER_ID, CREATOR_ID, 4);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(result.getPayerId()).isEqualTo(PAYER_ID);
        verify(groupAccessService).requireMember(GROUP_ID, CREATOR_ID);
        verify(eventParticipantRepository, times(1)).save(any(EventParticipant.class));
    }

    /** Запрещает создать сбор, если плательщик не состоит в группе. */
    @Test
    void shouldRejectCreateWhenPayerNotGroupMember() {
        when(userService.findById(CREATOR_ID)).thenReturn(Optional.of(createUser()));
        when(userService.findById(OTHER_MEMBER_ID))
                .thenReturn(Optional.of(createUser().toBuilder().id(OTHER_MEMBER_ID).build()));
        when(groupAccessService.isMember(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> eventService.create(GROUP_ID, "Dinner", null, OTHER_MEMBER_ID, CREATOR_ID, 4))
                .isInstanceOf(DomainException.class)
                .hasMessage("Payer must be a group member")
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    /** Меняет плательщика и ожидаемое число участников в черновике. */
    @Test
    void shouldUpdatePayerInDraftEvent() {
        var existing = event(100L, EventStatus.DRAFT);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(groupAccessService.isMember(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(true);
        when(userService.findById(OTHER_MEMBER_ID))
                .thenReturn(Optional.of(createUser().toBuilder().id(OTHER_MEMBER_ID).build()));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(false);
        when(eventParticipantRepository.countByEventId(100L)).thenReturn(2L);
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.update(100L, CREATOR_ID, "Dinner", "New desc", OTHER_MEMBER_ID, 4);

        assertThat(existing.getPayerId()).isEqualTo(OTHER_MEMBER_ID);
        assertThat(existing.getExpectedParticipantCount()).isEqualTo(4);
    }

    /** Запрещает присоединиться, если свободных мест нет. */
    @Test
    void shouldRejectJoinWhenEventIsFull() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        existing.setExpectedParticipantCount(2);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(false);
        when(eventParticipantRepository.countByEventId(100L)).thenReturn(2L);

        assertThatThrownBy(() -> eventService.join(100L, OTHER_MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Event has no free seats");
    }

    /** Разрешает присоединиться к рассчитанному сбору, даже если чужой перевод уже отмечен. */
    @Test
    void shouldJoinCalculatedEventWhenAnotherParticipantAlreadyPaid() {
        var existing = event(100L, EventStatus.CALCULATED);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(false);
        when(eventParticipantRepository.countByEventId(100L)).thenReturn(3L);

        var result = eventService.join(100L, OTHER_MEMBER_ID);

        assertThat(result.getStatus()).isEqualTo(EventStatus.CALCULATED);
        verify(eventParticipantRepository).save(any(EventParticipant.class));
    }

    /** Запрещает присоединиться к закрытому сбору. */
    @Test
    void shouldRejectJoinWhenEventIsCompleted() {
        var existing = event(100L, EventStatus.COMPLETED);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> eventService.join(100L, OTHER_MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cannot join this event");
    }

    /** Помечает сбор удалённым в любом статусе, если запрос от плательщика. */
    @Test
    void shouldDeleteEventInAnyStatusWhenRequesterIsPayer() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));

        eventService.delete(100L, PAYER_ID);

        verify(eventRepository).markDeleted(eq(100L), any(), any());
    }

    /** Помечает сбор удалённым по запросу владельца группы. */
    @Test
    void shouldDeleteEventWhenRequesterIsGroupOwner() {
        var existing = event(100L, EventStatus.COMPLETED);
        long ownerId = 9L;
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));

        eventService.delete(100L, ownerId);

        verify(groupAccessService).requireOwner(GROUP_ID, ownerId);
        verify(eventRepository).markDeleted(eq(100L), any(), any());
    }

    /** Запрещает удалять сбор обычному участнику. */
    @Test
    void shouldRejectDeleteForRegularMember() {
        var existing = event(100L, EventStatus.DRAFT);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        doThrow(new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Group owner access required"))
                .when(groupAccessService)
                .requireOwner(GROUP_ID, OTHER_MEMBER_ID);

        assertThatThrownBy(() -> eventService.delete(100L, OTHER_MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Event delete access required")
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTHORIZATION_ERROR);
        verify(eventRepository, never()).markDeleted(anyLong(), any(), any());
    }

    /** Помечает все сборы группы удалёнными перед удалением группы. */
    @Test
    void shouldSoftDeleteAllEventsWhenPreparingGroupDeletion() {
        var draft = event(1L, EventStatus.DRAFT);
        var live = event(2L, EventStatus.DISTRIBUTION);
        when(eventRepository.findByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of(draft, live));

        eventService.prepareGroupForDeletion(GROUP_ID);

        verify(eventRepository).markDeleted(eq(1L), any(), any());
        verify(eventRepository).markDeleted(eq(2L), any(), any());
    }

    /** Запрещает выйти из группы плательщику активного сбора. */
    @Test
    void shouldRejectGroupMemberCleanupWhenUserIsPayer() {
        when(eventRepository.existsByGroupIdAndPayerId(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.assertUserIsNotPayerOfActiveEvents(GROUP_ID, OTHER_MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Нельзя выйти из группы: вы плательщик в незакрытом сборе")
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    /** Удаляет обычного участника по запросу владельца группы. */
    @Test
    void shouldRemoveRegularParticipantByGroupOwner() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(true);

        eventService.removeParticipant(100L, CREATOR_ID, OTHER_MEMBER_ID);

        verify(eventSelectionCleanupPort).removeSelectionsForUser(100L, OTHER_MEMBER_ID);
        verify(eventParticipantRepository).deleteByEventIdAndUserId(100L, OTHER_MEMBER_ID);
    }

    /** Запрещает удалить из сбора плательщика. */
    @Test
    void shouldRejectRemovePayerFromEvent() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, PAYER_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.removeParticipant(100L, CREATOR_ID, PAYER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cannot remove this participant from the event");
    }

    /** Возвращает сбор из распределения в черновик и сбрасывает отметки выбора. */
    @Test
    void shouldRevertDistributionEventToDraft() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        var participant = participant(100L, OTHER_MEMBER_ID, Instant.parse("2026-01-01T00:00:00Z"));
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.findByEventId(100L)).thenReturn(List.of(participant));
        when(eventRepository.save(existing)).thenReturn(existing);

        var result = eventService.revertToDraft(100L, PAYER_ID);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(existing.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(participant.getSelectionCompletedAt()).isNull();
        verify(eventSelectionCleanupPort).removeSelectionsForEvent(100L);
        verify(eventDebtCleanupPort).deleteUnpaidDebts(100L);
        verify(eventParticipantRepository).save(participant);
    }

    /** Разрешает создателю вернуть рассчитанный сбор в черновик. */
    @Test
    void shouldRevertCalculatedEventWhenRequesterIsCreator() {
        var existing = event(100L, EventStatus.CALCULATED);
        existing.setPayerId(9L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.findByEventId(100L)).thenReturn(List.of());
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.revertToDraft(100L, CREATOR_ID);

        assertThat(existing.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventSelectionCleanupPort).removeSelectionsForEvent(100L);
        verify(eventDebtCleanupPort).deleteUnpaidDebts(100L);
    }

    /** Не меняет сбор, который уже находится в черновике. */
    @Test
    void shouldKeepDraftEventUnchangedOnRevert() {
        var existing = event(100L, EventStatus.DRAFT);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));

        var result = eventService.revertToDraft(100L, PAYER_ID);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository, never()).save(any());
        verify(eventSelectionCleanupPort, never()).removeSelectionsForEvent(100L);
        verify(eventDebtCleanupPort, never()).deleteUnpaidDebts(100L);
    }

    /** Запрещает вернуть в черновик закрытый сбор. */
    @Test
    void shouldRejectRevertWhenEventIsCompleted() {
        var existing = event(100L, EventStatus.COMPLETED);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> eventService.revertToDraft(100L, PAYER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
        verify(eventSelectionCleanupPort, never()).removeSelectionsForEvent(100L);
    }

    /** Запрещает обычному участнику возвращать сбор в черновик. */
    @Test
    void shouldRejectRevertForRegularParticipant() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> eventService.revertToDraft(100L, OTHER_MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTHORIZATION_ERROR);
        verify(eventDebtLockPort, never()).hasLockedDebts(100L);
    }

    /** Запрещает вернуть сбор в черновик, если есть заблокированные долги. */
    @Test
    void shouldRejectRevertWhenDebtsAreLocked() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventDebtLockPort.hasLockedDebts(100L)).thenReturn(true);

        assertThatThrownBy(() -> eventService.revertToDraft(100L, PAYER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Event can be returned to draft only before payments are confirmed");
        verify(eventDebtCleanupPort, never()).deleteUnpaidDebts(100L);
    }

    /** Запрещает выйти из группы, если перевод пользователя ещё не подтверждён. */
    @Test
    void shouldRejectGroupLeaveWhenUserTransferIsUnconfirmed() {
        var existing = event(100L, EventStatus.CALCULATED);
        when(eventRepository.findByGroupIdOrderByCreatedAtDesc(GROUP_ID)).thenReturn(List.of(existing));
        when(eventDebtCleanupPort.userHasUnconfirmedTransfer(OTHER_MEMBER_ID, List.of(100L))).thenReturn(true);

        assertThatThrownBy(() -> eventService.assertUserHasNoUnconfirmedTransfer(GROUP_ID, OTHER_MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Нельзя выйти: вы уже отметили перевод, а плательщик его ещё не подтвердил")
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    /** Запрещает плательщику выйти из сбора. */
    @Test
    void shouldRejectLeaveForPayer() {
        var existing = event(100L, EventStatus.DISTRIBUTION);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, PAYER_ID)).thenReturn(true);

        assertThatThrownBy(() -> eventService.leave(100L, PAYER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Cannot leave this event");
    }

    /** Разрешает выйти, если неподтверждённый перевод есть только у других участников. */
    @Test
    void shouldLeaveWhenOtherParticipantsAlreadyMarkedTransfers() {
        var existing = event(100L, EventStatus.CALCULATED);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(true);
        when(eventDebtCleanupPort.userHasUnconfirmedTransfer(OTHER_MEMBER_ID, List.of(100L))).thenReturn(false);

        var result = eventService.leave(100L, OTHER_MEMBER_ID);

        assertThat(result.getStatus()).isEqualTo(EventStatus.CALCULATED);
        verify(eventDebtLockPort, never()).hasLockedDebts(100L);
        verify(eventDebtCleanupPort).deleteUnpaidDebt(100L, OTHER_MEMBER_ID);
        verify(eventParticipantRepository).deleteByEventIdAndUserId(100L, OTHER_MEMBER_ID);
    }

    /** Запрещает выйти, если собственный перевод ещё не подтверждён. */
    @Test
    void shouldRejectLeaveWhenOwnTransferIsUnconfirmed() {
        var existing = event(100L, EventStatus.CALCULATED);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, OTHER_MEMBER_ID)).thenReturn(true);
        when(eventDebtCleanupPort.userHasUnconfirmedTransfer(OTHER_MEMBER_ID, List.of(100L))).thenReturn(true);

        assertThatThrownBy(() -> eventService.leave(100L, OTHER_MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Нельзя выйти: вы уже отметили перевод, а плательщик его ещё не подтвердил");
        verify(eventParticipantRepository, never()).deleteByEventIdAndUserId(100L, OTHER_MEMBER_ID);
    }

    private static EventParticipant participant(long eventId, long userId, Instant selectionCompletedAt) {
        return EventParticipant.builder()
                .eventId(eventId)
                .userId(userId)
                .selectionCompletedAt(selectionCompletedAt)
                .build();
    }

    private static Event event(long id, EventStatus status) {
        return Event.builder()
                .id(id)
                .groupId(GROUP_ID)
                .name("Dinner")
                .payerId(PAYER_ID)
                .createdBy(CREATOR_ID)
                .status(status)
                .expectedParticipantCount(4)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
