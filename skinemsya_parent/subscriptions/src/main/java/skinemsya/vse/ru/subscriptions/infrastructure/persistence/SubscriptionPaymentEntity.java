package skinemsya.vse.ru.subscriptions.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionPaymentKind;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionPaymentStatus;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionProvider;

@Entity
@Table(name = "subscription_payments")
public class SubscriptionPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SubscriptionProvider provider;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "amount_kopecks", nullable = false)
    private long amountKopecks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionPaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private SubscriptionPaymentKind kind;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SubscriptionProvider getProvider() {
        return provider;
    }

    public void setProvider(SubscriptionProvider provider) {
        this.provider = provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public long getAmountKopecks() {
        return amountKopecks;
    }

    public void setAmountKopecks(long amountKopecks) {
        this.amountKopecks = amountKopecks;
    }

    public SubscriptionPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionPaymentStatus status) {
        this.status = status;
    }

    public SubscriptionPaymentKind getKind() {
        return kind;
    }

    public void setKind(SubscriptionPaymentKind kind) {
        this.kind = kind;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
