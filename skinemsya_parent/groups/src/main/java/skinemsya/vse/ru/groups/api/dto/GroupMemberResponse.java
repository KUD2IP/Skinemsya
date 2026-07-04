package skinemsya.vse.ru.groups.api.dto;

import skinemsya.vse.ru.groups.domain.GroupRole;

import java.time.Instant;

public record GroupMemberResponse(
        long id,
        long groupId,
        long userId,
        GroupRole role,
        Instant joinedAt
) {
}
