package skinemsya.vse.ru.app.predictions.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evening_prediction_draws")
public class EveningDrawEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_chat_id", nullable = false)
    private long telegramChatId;

    @Column(name = "frame_id", nullable = false, length = 40)
    private String frameId;

    @Column(name = "target_telegram_user_id", nullable = false)
    private long targetTelegramUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getFrameId() {
        return frameId;
    }

    public void setFrameId(String frameId) {
        this.frameId = frameId;
    }

    public long getTargetTelegramUserId() {
        return targetTelegramUserId;
    }

    public void setTargetTelegramUserId(long targetTelegramUserId) {
        this.targetTelegramUserId = targetTelegramUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
