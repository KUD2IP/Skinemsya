package skinemsya.vse.ru.app.predictions.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evening_chat_speakers")
public class EveningSpeakerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_chat_id", nullable = false)
    private long telegramChatId;

    @Column(name = "telegram_user_id", nullable = false)
    private long telegramUserId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public Long getId() {
        return id;
    }

    public long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public long getTelegramUserId() {
        return telegramUserId;
    }

    public void setTelegramUserId(long telegramUserId) {
        this.telegramUserId = telegramUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
