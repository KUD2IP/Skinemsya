package skinemsya.vse.ru.receipts.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import skinemsya.vse.ru.common.event.SelectionsCompleted;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.debts.domain.Debt;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.receipts.application.PositionAvailabilityService.PositionAvailability;
import skinemsya.vse.ru.receipts.domain.exception.SelectionCannotBeReopenedException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;

@ExtendWith(MockitoExtension.class)
class SelectionServiceImplTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionSelectionRepository selectionRepository;

    @Mock
    private EventAccessPort eventAccessPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DebtService debtService;

    @Mock
    private PositionAvailabilityService positionAvailabilityService;

    @InjectMocks
    private SelectionServiceImpl selectionService;

    @Test
    void shouldReopenSelectionWhenDebtsAreUnpaid() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.CALCULATED);
        when(debtService.findByEvent(10L)).thenReturn(List.of(unpaidDebt(10L, 2L)));

        selectionService.reopenSelection(10L, 2L);

        verify(eventAccessPort).requireParticipant(10L, 2L);
        verify(eventAccessPort).clearSelectionCompleted(10L, 2L);
        verify(debtService).recalculateUnpaidDebts(10L);
    }

    @Test
    void shouldRejectReopenWhenPaymentAlreadyStarted() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.CALCULATED);
        when(debtService.findByEvent(10L)).thenReturn(List.of(debt(10L, 2L, DebtStatus.PENDING_CONFIRMATION)));

        assertThatThrownBy(() -> selectionService.reopenSelection(10L, 2L))
                .isInstanceOf(SelectionCannotBeReopenedException.class);

        verify(eventAccessPort, never()).clearSelectionCompleted(10L, 2L);
        verify(debtService, never()).recalculateUnpaidDebts(10L);
    }

    @Test
    void shouldRejectReopenWhenEventIsCompleted() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.COMPLETED);

        assertThatThrownBy(() -> selectionService.reopenSelection(10L, 2L))
                .isInstanceOf(SelectionCannotBeReopenedException.class);

        verify(eventAccessPort, never()).clearSelectionCompleted(10L, 2L);
    }

    @Test
    void shouldAutoCompleteSoldOutParticipantsAndCalculate() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.DISTRIBUTION);
        when(eventAccessPort.getIncompleteSelectionParticipantUserIds(10L)).thenReturn(List.of(3L));
        var position = position(1L, false);
        when(positionRepository.findByEventIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(position));
        when(positionAvailabilityService.availabilityFor(position, 3L)).thenReturn(new PositionAvailability(1, 0, 0, true));
        when(eventAccessPort.allSelectionsCompleted(10L)).thenReturn(true);

        selectionService.completeSelection(10L, 2L);

        verify(eventAccessPort).markSelectionCompleted(10L, 2L);
        verify(debtService).upsertDebtForParticipant(10L, 2L);
        verify(eventAccessPort).markSelectionCompleted(10L, 3L);
        verify(debtService).upsertDebtForParticipant(10L, 3L);
        verify(eventPublisher).publishEvent(new SelectionsCompleted(10L));
    }

    @Test
    void shouldNotAutoCompleteWhenSharedPositionsExist() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.DISTRIBUTION);
        when(eventAccessPort.getIncompleteSelectionParticipantUserIds(10L)).thenReturn(List.of(3L));
        var regular = position(1L, false);
        var shared = position(2L, true);
        when(positionRepository.findByEventIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(regular, shared));
        when(positionAvailabilityService.availabilityFor(regular, 3L)).thenReturn(new PositionAvailability(1, 0, 0, true));
        when(eventAccessPort.allSelectionsCompleted(10L)).thenReturn(false);

        selectionService.completeSelection(10L, 2L);

        verify(eventAccessPort, never()).markSelectionCompleted(10L, 3L);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldNotAutoCompleteWhenRemainingQuantityExists() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.DISTRIBUTION);
        when(eventAccessPort.getIncompleteSelectionParticipantUserIds(10L)).thenReturn(List.of(3L));
        var position = position(1L, false);
        when(positionRepository.findByEventIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(position));
        when(positionAvailabilityService.availabilityFor(position, 3L))
                .thenReturn(new PositionAvailability(2, 1, 0, false));
        when(eventAccessPort.allSelectionsCompleted(10L)).thenReturn(false);

        selectionService.completeSelection(10L, 2L);

        verify(eventAccessPort, never()).markSelectionCompleted(10L, 3L);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldReplacePreviousSelectionsAndNotCalculateOnUpdate() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.DISTRIBUTION);
        var coffee = position(1L, false);
        var tea = position(2L, false);
        when(positionRepository.findByEventIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(coffee, tea));
        when(positionRepository.findByIdAndEventId(2L, 10L)).thenReturn(Optional.of(tea));
        when(selectionRepository.findByPositionIdAndUserId(2L, 2L)).thenReturn(Optional.empty());

        selectionService.updateSelections(10L, 2L, List.of(new SelectionService.SelectionUpdate(2L, BigDecimal.ONE)));

        verify(selectionRepository).deleteByUserIdAndPositionIdIn(2L, List.of(1L));
        verify(selectionRepository).save(any(PositionSelectionEntity.class));
        verify(eventPublisher, never()).publishEvent(any());
        verify(eventAccessPort, never()).getIncompleteSelectionParticipantUserIds(anyLong());
    }

    @Test
    void shouldClearAllSelectionsWhenUpdateIsEmpty() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.DISTRIBUTION);
        when(positionRepository.findByEventIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(position(1L, false), position(2L, false)));

        selectionService.updateSelections(10L, 2L, List.of());

        verify(selectionRepository).deleteByUserIdAndPositionIdIn(2L, List.of(1L, 2L));
        verify(selectionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static PositionEntity position(long id, boolean shared) {
        var entity = new PositionEntity();
        entity.setId(id);
        entity.setShared(shared);
        return entity;
    }

    private static Debt unpaidDebt(long eventId, long debtorId) {
        return debt(eventId, debtorId, DebtStatus.UNPAID);
    }

    private static Debt debt(long eventId, long debtorId, DebtStatus status) {
        var now = Instant.now();
        return new Debt(1L, eventId, debtorId, 1L, 30000L, status, now, now);
    }
}
