package skinemsya.vse.ru.groups.domain;

import java.time.Instant;

public record GroupMember(
        long id,
        long groupId,
        long userId,
        GroupRole role,
        Instant joinedAt
) {
}
