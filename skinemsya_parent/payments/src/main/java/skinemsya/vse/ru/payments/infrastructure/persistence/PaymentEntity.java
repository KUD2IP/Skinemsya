package skinemsya.vse.ru.payments.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import skinemsya.vse.ru.payments.domain.PaymentStatus;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debt_id", nullable = false, unique = true)
    private Long debtId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "screenshot_file_id")
    private Long screenshotFileId;

    @Column(name = "debtor_confirmed_at")
    private Instant debtorConfirmedAt;

    @Column(name = "payer_confirmed_at")
    private Instant payerConfirmedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDebtId() {
        return debtId;
    }

    public void setDebtId(Long debtId) {
        this.debtId = debtId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Long getScreenshotFileId() {
        return screenshotFileId;
    }

    public void setScreenshotFileId(Long screenshotFileId) {
        this.screenshotFileId = screenshotFileId;
    }

    public Instant getDebtorConfirmedAt() {
        return debtorConfirmedAt;
    }

    public void setDebtorConfirmedAt(Instant debtorConfirmedAt) {
        this.debtorConfirmedAt = debtorConfirmedAt;
    }

    public Instant getPayerConfirmedAt() {
        return payerConfirmedAt;
    }

    public void setPayerConfirmedAt(Instant payerConfirmedAt) {
        this.payerConfirmedAt = payerConfirmedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
