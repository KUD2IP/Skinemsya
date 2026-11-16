package skinemsya.vse.ru.impl.service.debts;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.event.events.EventParticipantsChanged;
import skinemsya.vse.ru.api.internal.service.debts.DebtService;

/** Пересчитывает неоплаченные долги при изменении состава сбора. */
@Component
@RequiredArgsConstructor
public class EventParticipantsChangedListener {

    private final DebtService debtService;

    /** Пересчитывает неоплаченные долги сбора. */
    @EventListener
    @Transactional
    public void onEventParticipantsChanged(EventParticipantsChanged event) {
        debtService.recalculateUnpaidDebts(event.eventId());
    }
}
