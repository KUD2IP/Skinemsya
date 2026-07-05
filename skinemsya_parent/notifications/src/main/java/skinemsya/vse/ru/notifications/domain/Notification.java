package skinemsya.vse.ru.notifications.domain;

import java.time.Instant;

public record Notification(
        long id,
        long userId,
        NotificationType type,
        String payload,
        NotificationStatus status,
        Instant sentAt,
        Instant createdAt) {}
