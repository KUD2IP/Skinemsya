package skinemsya.vse.ru.impl.service.debts;

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
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.api.internal.service.events.EventAccessPort;
import skinemsya.vse.ru.api.internal.service.receipts.ReceiptDataPort;

/** Тесты пересчёта неоплаченных долгов. */
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

    /** Возвращает рассчитанный сбор в распределение и не меняет уже оплаченный долг. */
    @Test
    void shouldReopenCalculatedEventWithoutChangingPaidDebt() {
        when(eventAccessPort.getStatus(10L)).thenReturn(EventStatus.CALCULATED, EventStatus.DISTRIBUTION);
        when(eventAccessPort.getSelectionCompletedParticipantUserIds(10L)).thenReturn(List.of(2L));
        when(eventAccessPort.getPayerId(10L)).thenReturn(1L);
        var paid = Debt.builder().status(DebtStatus.PAID).amountKopecks(5_000L).build();
        when(debtRepository.findByEventIdAndDebtorId(10L, 2L)).thenReturn(Optional.of(paid));

        debtService.recalculateUnpaidDebts(10L);

        verify(eventAccessPort).revertCalculatedToDistribution(10L);
        verify(debtRepository).deleteByEventIdAndStatus(10L, DebtStatus.UNPAID);
        verify(debtRepository, never()).save(any());
    }
}
