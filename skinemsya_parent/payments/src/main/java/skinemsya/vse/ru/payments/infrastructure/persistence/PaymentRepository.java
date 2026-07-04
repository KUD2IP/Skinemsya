package skinemsya.vse.ru.payments.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.payments.domain.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByDebtId(long debtId);

    List<PaymentEntity> findByDebtIdIn(List<Long> debtIds);

    List<PaymentEntity> findByStatus(PaymentStatus status);
}
