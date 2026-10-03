package skinemsya.vse.ru.debts.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtEntity;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;

@ExtendWith(MockitoExtension.class)
class DebtServiceImplRecalculateTest {

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private ReceiptDataPort receiptDataPort;

    @Mock
    private EventAccessPort eventAccessPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DebtServiceImpl debtService;

    @Test
    void shouldReopenCalculatedEventWithoutChangingPaidDebt() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.CALCULATED, EventStatus.DISTRIBUTION);
        when(eventAccessPort.getSelectionCompletedParticipantUserIds(10L)).thenReturn(List.of(2L));
        when(eventAccessPort.getPayerId(10L)).thenReturn(1L);
        var paid = new DebtEntity();
        paid.setStatus(DebtStatus.PAID);
        paid.setAmountKopecks(5_000L);
        when(debtRepository.findByEventIdAndDebtorId(10L, 2L)).thenReturn(Optional.of(paid));

        debtService.recalculateUnpaidDebts(10L);

        verify(eventAccessPort).revertCalculatedToDistribution(10L);
        verify(debtRepository).deleteByEventIdAndStatus(10L, DebtStatus.UNPAID);
        verify(debtRepository, never()).save(any());
    }
}
