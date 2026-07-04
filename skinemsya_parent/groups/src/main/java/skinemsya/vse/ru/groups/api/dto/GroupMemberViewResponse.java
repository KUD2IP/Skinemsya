package skinemsya.vse.ru.groups.api.dto;

import skinemsya.vse.ru.groups.domain.GroupRole;

import java.time.Instant;

public record GroupMemberViewResponse(
        long id,
        long groupId,
        long userId,
        GroupRole role,
        String displayName,
        String telegramUsername,
        long telegramUserId,
        Instant joinedAt
) {
}
