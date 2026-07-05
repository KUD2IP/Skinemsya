package skinemsya.vse.ru.payments.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayerReminderJobRepository extends JpaRepository<PayerReminderJobEntity, Long> {

    List<PayerReminderJobEntity> findBySentAtIsNullAndScheduledAtBefore(Instant before);

    Optional<PayerReminderJobEntity> findByPaymentId(long paymentId);
}
