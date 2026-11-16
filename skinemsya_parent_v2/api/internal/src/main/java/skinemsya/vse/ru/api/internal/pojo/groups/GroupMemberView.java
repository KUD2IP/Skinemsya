package skinemsya.vse.ru.api.internal.pojo.groups;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;

/** Участник группы с данными пользователя для отображения. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberView {

    private long id;
    private long groupId;
    private long userId;
    private GroupRole role;
    private String displayName;
    private String telegramUsername;
    private long telegramUserId;
    private Instant joinedAt;
    private String photoUrl;
}
