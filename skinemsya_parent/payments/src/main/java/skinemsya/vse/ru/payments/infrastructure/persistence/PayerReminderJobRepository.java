package skinemsya.vse.ru.payments.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PayerReminderJobRepository extends JpaRepository<PayerReminderJobEntity, Long> {

    List<PayerReminderJobEntity> findBySentAtIsNullAndScheduledAtBefore(Instant before);

    Optional<PayerReminderJobEntity> findByPaymentId(long paymentId);
}
