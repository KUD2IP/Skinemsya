package skinemsya.vse.ru.debts.application;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.debts.domain.exception.UnpaidDebtsRemainException;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.events.application.EventCloseReadinessPort;

@Component
@Primary
public class EventCloseReadinessPortImpl implements EventCloseReadinessPort {

    private final DebtRepository debtRepository;

    public EventCloseReadinessPortImpl(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    @Override
    public void assertReadyToClose(long eventId) {
        if (debtRepository.existsByEventIdAndStatusNot(eventId, DebtStatus.PAID)) {
            throw new UnpaidDebtsRemainException();
        }
    }
}
