package skinemsya.vse.ru.debts.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.debts.domain.DebtStatus;

import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<DebtEntity, Long> {

    List<DebtEntity> findByEventId(long eventId);

    List<DebtEntity> findByDebtorId(long debtorId);

    List<DebtEntity> findByCreditorId(long creditorId);

    long countByEventIdAndStatusNot(long eventId, DebtStatus status);

    boolean existsByEventIdAndStatusNot(long eventId, DebtStatus status);

    void deleteByEventId(long eventId);

    void deleteByEventIdAndStatus(long eventId, DebtStatus status);

    Optional<DebtEntity> findByEventIdAndDebtorId(long eventId, long debtorId);

    boolean existsByEventIdAndStatusIn(long eventId, List<DebtStatus> statuses);

    Optional<DebtEntity> findByIdAndEventId(long id, long eventId);
}
