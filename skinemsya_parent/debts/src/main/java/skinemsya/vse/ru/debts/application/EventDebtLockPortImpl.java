package skinemsya.vse.ru.debts.application;

import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.events.application.EventDebtLockPort;

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
