package skinemsya.vse.ru.api.internal.pojo.groups;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;

/** Внутреннее представление участника группы. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    private long id;
    private long groupId;
    private long userId;
    private GroupRole role;
    private Instant joinedAt;
}
