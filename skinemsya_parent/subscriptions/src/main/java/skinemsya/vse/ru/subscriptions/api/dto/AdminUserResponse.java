package skinemsya.vse.ru.subscriptions.api.dto;

import java.time.Instant;

public record AdminUserResponse(
        long id,
        String displayName,
        String telegramUsername,
        Instant createdAt,
        Instant currentPeriodEnd,
        boolean active,
        String provider,
        int eventsCreated,
        int receiptCount) {}
