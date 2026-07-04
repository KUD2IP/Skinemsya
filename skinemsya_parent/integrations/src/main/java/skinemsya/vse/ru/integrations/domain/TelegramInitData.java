package skinemsya.vse.ru.integrations.domain;

import java.util.Optional;

public record TelegramInitData(
        TelegramIdentity identity,
        Optional<TelegramChatContext> chat
) {
}
