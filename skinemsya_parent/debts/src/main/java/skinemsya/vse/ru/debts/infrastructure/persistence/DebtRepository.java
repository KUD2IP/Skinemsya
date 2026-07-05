package skinemsya.vse.ru.debts.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.debts.domain.DebtStatus;

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
