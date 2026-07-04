package skinemsya.vse.ru.debts.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.SelectionsCompleted;

@Component
public class SelectionsCompletedListener {

    private final DebtService debtService;

    public SelectionsCompletedListener(DebtService debtService) {
        this.debtService = debtService;
    }

    @EventListener
    @Transactional
    public void onSelectionsCompleted(SelectionsCompleted event) {
        debtService.calculate(event.eventId());
    }
}
