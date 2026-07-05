package skinemsya.vse.ru.groups.api.dto;

import java.time.Instant;
import skinemsya.vse.ru.groups.domain.GroupType;

public record GroupResponse(
        long id,
        String name,
        GroupType type,
        Long telegramChatId,
        long ownerId,
        Instant createdAt,
        Instant updatedAt) {}
