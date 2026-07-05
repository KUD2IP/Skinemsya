package skinemsya.vse.ru.groups.domain;

import java.time.Instant;

public record GroupMemberView(
        long id,
        long groupId,
        long userId,
        GroupRole role,
        String displayName,
        String telegramUsername,
        long telegramUserId,
        Instant joinedAt) {}
