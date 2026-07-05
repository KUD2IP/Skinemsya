package skinemsya.vse.ru.groups.api.dto;

import java.time.Instant;
import skinemsya.vse.ru.groups.domain.GroupRole;

public record GroupMemberViewResponse(
        long id,
        long groupId,
        long userId,
        GroupRole role,
        String displayName,
        String telegramUsername,
        long telegramUserId,
        Instant joinedAt) {}
