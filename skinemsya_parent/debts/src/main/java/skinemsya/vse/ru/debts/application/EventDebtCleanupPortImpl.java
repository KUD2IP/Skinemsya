package skinemsya.vse.ru.debts.application;

import java.util.Collection;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.debts.infrastructure.persistence.DebtRepository;
import skinemsya.vse.ru.events.application.EventDebtCleanupPort;

@Component
@Primary
public class EventDebtCleanupPortImpl implements EventDebtCleanupPort {

    private final DebtRepository debtRepository;

    public EventDebtCleanupPortImpl(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    @Override
    public void deleteUnpaidDebts(long eventId) {
        debtRepository.deleteByEventIdAndStatus(eventId, DebtStatus.UNPAID);
    }

    @Override
    public void deleteUnpaidDebt(long eventId, long userId) {
        debtRepository.deleteByEventIdAndDebtorIdAndStatus(eventId, userId, DebtStatus.UNPAID);
    }

    @Override
    public boolean userHasUnconfirmedTransfer(long userId, Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return false;
        }
        for (long eventId : eventIds) {
            if (debtRepository.existsByDebtorIdAndEventIdAndStatus(
                    userId, eventId, DebtStatus.PENDING_CONFIRMATION)) {
                return true;
            }
        }
        return false;
    }
}
