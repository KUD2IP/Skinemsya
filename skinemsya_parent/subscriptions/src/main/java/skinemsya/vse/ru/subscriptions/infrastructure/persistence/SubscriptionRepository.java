package skinemsya.vse.ru.subscriptions.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionProvider;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    Optional<SubscriptionEntity> findByUserId(long userId);

    @Query(
            """
            SELECT s FROM SubscriptionEntity s
            WHERE s.provider = :provider
              AND s.cancelAtPeriodEnd = false
              AND s.yookassaPaymentMethodId IS NOT NULL
              AND s.currentPeriodEnd IS NOT NULL
              AND s.currentPeriodEnd >= :since
              AND s.currentPeriodEnd <= :until
            """)
    List<SubscriptionEntity> findDue(SubscriptionProvider provider, Instant since, Instant until);
}
