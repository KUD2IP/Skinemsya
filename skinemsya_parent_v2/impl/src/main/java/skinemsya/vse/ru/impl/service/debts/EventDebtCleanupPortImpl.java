package skinemsya.vse.ru.impl.service.debts;

import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.api.internal.service.events.EventDebtCleanupPort;

/** Удаляет неоплаченные долги и ищет неподтверждённые переводы. */
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
            if (debtRepository.existsByDebtorIdAndEventIdAndStatus(userId, eventId, DebtStatus.PENDING_CONFIRMATION)) {
                return true;
            }
        }
        return false;
    }
}
