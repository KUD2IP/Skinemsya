package skinemsya.vse.ru.integrations.domain;

import java.time.Instant;

public record TelegramIdentity(
        long telegramUserId,
        String displayName,
        Instant authDate,
        String telegramUsername
) {
}
