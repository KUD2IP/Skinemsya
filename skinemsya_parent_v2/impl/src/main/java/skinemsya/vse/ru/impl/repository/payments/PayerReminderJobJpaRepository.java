package skinemsya.vse.ru.impl.repository.payments;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.data.entity.payments.PayerReminderJobEntity;

/** Spring Data репозиторий напоминаний плательщику. */
public interface PayerReminderJobJpaRepository extends JpaRepository<PayerReminderJobEntity, Long> {

    List<PayerReminderJobEntity> findBySentAtIsNullAndScheduledAtBefore(Instant before);

    Optional<PayerReminderJobEntity> findByPaymentId(long paymentId);
}
