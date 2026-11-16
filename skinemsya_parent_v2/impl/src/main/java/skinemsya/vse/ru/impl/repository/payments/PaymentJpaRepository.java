package skinemsya.vse.ru.impl.repository.payments;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;
import skinemsya.vse.ru.data.entity.payments.PaymentEntity;

/** Spring Data репозиторий платежей. */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByDebtId(long debtId);

    List<PaymentEntity> findByDebtIdIn(Collection<Long> debtIds);

    Optional<PaymentEntity> findByScreenshotFileId(long screenshotFileId);

    List<PaymentEntity> findByStatus(PaymentStatus status);
}
