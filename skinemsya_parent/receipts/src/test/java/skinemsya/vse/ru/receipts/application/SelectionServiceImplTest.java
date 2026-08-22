package skinemsya.vse.ru.receipts.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.debts.domain.Debt;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.receipts.domain.exception.SelectionCannotBeReopenedException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
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

    private static Debt unpaidDebt(long eventId, long debtorId) {
        return debt(eventId, debtorId, DebtStatus.UNPAID);
    }

    private static Debt debt(long eventId, long debtorId, DebtStatus status) {
        var now = Instant.now();
        return new Debt(1L, eventId, debtorId, 1L, 30000L, status, now, now);
    }
}
