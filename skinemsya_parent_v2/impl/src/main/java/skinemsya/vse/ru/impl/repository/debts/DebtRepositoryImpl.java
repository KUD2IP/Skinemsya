package skinemsya.vse.ru.impl.repository.debts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.repository.debts.DebtRepository;
import skinemsya.vse.ru.impl.mapper.entity.DebtEntityMapper;

/** Адаптер между внутренним портом долгов и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class DebtRepositoryImpl implements DebtRepository {

    private final DebtJpaRepository debtJpaRepository;
    private final DebtEntityMapper debtEntityMapper;

    @Override
    public Optional<Debt> findById(long debtId) {
        return debtJpaRepository.findById(debtId).map(debtEntityMapper::toDebt);
    }

    @Override
    public Optional<Debt> findByIdAndEventId(long id, long eventId) {
        return debtJpaRepository.findByIdAndEventId(id, eventId).map(debtEntityMapper::toDebt);
    }

    @Override
    public List<Debt> findByEventId(long eventId) {
        return debtJpaRepository.findByEventId(eventId).stream()
                .map(debtEntityMapper::toDebt)
                .toList();
    }

    @Override
    public List<Debt> findByDebtorId(long debtorId) {
        return debtJpaRepository.findByDebtorId(debtorId).stream()
                .map(debtEntityMapper::toDebt)
                .toList();
    }

    @Override
    public List<Debt> findByCreditorId(long creditorId) {
        return debtJpaRepository.findByCreditorId(creditorId).stream()
                .map(debtEntityMapper::toDebt)
                .toList();
    }

    @Override
    public Optional<Debt> findByEventIdAndDebtorId(long eventId, long debtorId) {
        return debtJpaRepository.findByEventIdAndDebtorId(eventId, debtorId).map(debtEntityMapper::toDebt);
    }

    @Override
    public long countByEventIdAndStatusNot(long eventId, DebtStatus status) {
        return debtJpaRepository.countByEventIdAndStatusNot(eventId, status);
    }

    @Override
    public boolean existsByEventIdAndStatusNot(long eventId, DebtStatus status) {
        return debtJpaRepository.existsByEventIdAndStatusNot(eventId, status);
    }

    @Override
    public boolean existsByEventIdAndStatusIn(long eventId, Collection<DebtStatus> statuses) {
        return debtJpaRepository.existsByEventIdAndStatusIn(eventId, statuses);
    }

    @Override
    public boolean existsByDebtorIdAndEventIdAndStatus(long debtorId, long eventId, DebtStatus status) {
        return debtJpaRepository.existsByDebtorIdAndEventIdAndStatus(debtorId, eventId, status);
    }

    @Override
    public boolean existsByDebtorIdAndEventIdInAndStatusIn(
            long debtorId, Collection<Long> eventIds, Collection<DebtStatus> statuses) {
        return debtJpaRepository.existsByDebtorIdAndEventIdInAndStatusIn(debtorId, eventIds, statuses);
    }

    @Override
    public Debt save(Debt debt) {
        var entity = debtEntityMapper.toEntity(debt);
        if (debt.getId() == 0L) {
            entity.setId(null);
        }
        return debtEntityMapper.toDebt(debtJpaRepository.save(entity));
    }

    @Override
    public void delete(Debt debt) {
        debtJpaRepository.deleteById(debt.getId());
    }

    @Override
    public void deleteByEventId(long eventId) {
        debtJpaRepository.deleteByEventId(eventId);
    }

    @Override
    public void deleteByEventIdAndStatus(long eventId, DebtStatus status) {
        debtJpaRepository.deleteByEventIdAndStatus(eventId, status);
    }

    @Override
    public void deleteByEventIdAndDebtorIdAndStatus(long eventId, long debtorId, DebtStatus status) {
        debtJpaRepository.deleteByEventIdAndDebtorIdAndStatus(eventId, debtorId, status);
    }
}
