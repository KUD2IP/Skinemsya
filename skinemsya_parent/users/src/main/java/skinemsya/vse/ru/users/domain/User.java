package skinemsya.vse.ru.users.domain;

import java.time.Instant;

public record User(
        long id,
        long telegramUserId,
        String displayName,
        String telegramUsername,
        Instant createdAt,
        Instant updatedAt
) {
}
