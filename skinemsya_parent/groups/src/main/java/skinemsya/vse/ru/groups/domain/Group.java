package skinemsya.vse.ru.groups.domain;

import java.time.Instant;

public record Group(
        long id,
        String name,
        GroupType type,
        Long telegramChatId,
        long ownerId,
        Instant createdAt,
        Instant updatedAt
) {
}
