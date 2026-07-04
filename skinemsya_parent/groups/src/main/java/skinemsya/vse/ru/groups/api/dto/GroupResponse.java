package skinemsya.vse.ru.groups.api.dto;

import skinemsya.vse.ru.groups.domain.GroupType;

import java.time.Instant;

public record GroupResponse(
        long id,
        String name,
        GroupType type,
        Long telegramChatId,
        long ownerId,
        Instant createdAt,
        Instant updatedAt
) {
}
