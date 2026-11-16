package skinemsya.vse.ru.impl.repository.debts;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.data.entity.debts.DebtEntity;

/** Spring Data репозиторий долгов. */
public interface DebtJpaRepository extends JpaRepository<DebtEntity, Long> {

    List<DebtEntity> findByEventId(long eventId);

    List<DebtEntity> findByDebtorId(long debtorId);

    List<DebtEntity> findByCreditorId(long creditorId);

    Optional<DebtEntity> findByEventIdAndDebtorId(long eventId, long debtorId);

    Optional<DebtEntity> findByIdAndEventId(long id, long eventId);

    long countByEventIdAndStatusNot(long eventId, DebtStatus status);

    boolean existsByEventIdAndStatusNot(long eventId, DebtStatus status);

    boolean existsByEventIdAndStatusIn(long eventId, Collection<DebtStatus> statuses);

    boolean existsByDebtorIdAndEventIdAndStatus(long debtorId, long eventId, DebtStatus status);

    boolean existsByDebtorIdAndEventIdInAndStatusIn(
            long debtorId, Collection<Long> eventIds, Collection<DebtStatus> statuses);

    void deleteByEventId(long eventId);

    void deleteByEventIdAndStatus(long eventId, DebtStatus status);

    void deleteByEventIdAndDebtorIdAndStatus(long eventId, long debtorId, DebtStatus status);
}
