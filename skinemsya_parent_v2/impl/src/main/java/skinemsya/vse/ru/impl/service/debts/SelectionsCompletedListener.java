package skinemsya.vse.ru.impl.service.debts;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.event.receipts.SelectionsCompleted;
import skinemsya.vse.ru.api.internal.service.debts.DebtService;

/** Запускает расчёт долгов, когда все участники закончили выбор. */
@Component
@RequiredArgsConstructor
public class SelectionsCompletedListener {

    private final DebtService debtService;

    /** Рассчитывает долги сбора. */
    @EventListener
    @Transactional
    public void onSelectionsCompleted(SelectionsCompleted event) {
        debtService.calculate(event.eventId());
    }
}
