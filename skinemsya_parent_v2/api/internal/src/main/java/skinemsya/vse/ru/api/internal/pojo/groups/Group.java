package skinemsya.vse.ru.api.internal.pojo.groups;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;

/** Внутреннее представление группы. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    private long id;
    private String name;
    private GroupType type;
    private Long telegramChatId;
    private long ownerId;
    private String inviteCode;
    private Instant createdAt;
    private Instant updatedAt;
}
