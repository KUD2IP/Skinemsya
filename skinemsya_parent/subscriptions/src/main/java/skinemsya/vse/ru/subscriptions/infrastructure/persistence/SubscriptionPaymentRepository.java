package skinemsya.vse.ru.subscriptions.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionPaymentKind;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionPaymentStatus;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionProvider;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPaymentEntity, Long> {

    Optional<SubscriptionPaymentEntity> findByExternalId(String externalId);

    List<SubscriptionPaymentEntity> findTop3ByUserIdAndProviderAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            long userId, SubscriptionProvider provider, SubscriptionPaymentStatus status, Instant createdAt);

    boolean existsByUserIdAndKindAndStatusAndCreatedAtAfter(
            long userId, SubscriptionPaymentKind kind, SubscriptionPaymentStatus status, Instant createdAt);
}
