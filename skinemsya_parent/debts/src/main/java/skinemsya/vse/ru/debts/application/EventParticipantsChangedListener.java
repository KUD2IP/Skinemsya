package skinemsya.vse.ru.debts.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.EventParticipantsChanged;

@Component
public class EventParticipantsChangedListener {

    private final DebtService debtService;

    public EventParticipantsChangedListener(DebtService debtService) {
        this.debtService = debtService;
    }

    @EventListener
    @Transactional
    public void onEventParticipantsChanged(EventParticipantsChanged event) {
        debtService.recalculateUnpaidDebts(event.eventId());
    }
}
