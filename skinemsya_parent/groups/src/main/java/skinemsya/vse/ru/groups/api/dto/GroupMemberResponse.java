package skinemsya.vse.ru.groups.api.dto;

import java.time.Instant;
import skinemsya.vse.ru.groups.domain.GroupRole;

public record GroupMemberResponse(long id, long groupId, long userId, GroupRole role, Instant joinedAt) {}
