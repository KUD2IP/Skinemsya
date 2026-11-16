package skinemsya.vse.ru.impl.service.debts;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.api.internal.service.events.EventCloseReadinessPort;

/** Запрещает закрыть сбор, пока остались неоплаченные долги. */
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
            throw new DomainException(ErrorCode.DOMAIN_CONFLICT, "All debts must be paid before closing the event");
        }
    }
}
