package skinemsya.vse.ru.impl.service.debts;

import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.api.internal.service.events.EventDebtLockPort;

/** Считает долг заблокированным, если перевод уже отмечен или оплачен. */
@Component
@Primary
public class EventDebtLockPortImpl implements EventDebtLockPort {

    private static final List<DebtStatus> LOCKED = List.of(DebtStatus.PENDING_CONFIRMATION, DebtStatus.PAID);

    private final DebtRepository debtRepository;

    public EventDebtLockPortImpl(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    @Override
    public boolean hasLockedDebts(long eventId) {
        return debtRepository.existsByEventIdAndStatusIn(eventId, LOCKED);
    }
}
