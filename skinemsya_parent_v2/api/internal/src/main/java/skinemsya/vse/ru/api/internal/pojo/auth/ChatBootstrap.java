package skinemsya.vse.ru.api.internal.pojo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.auth.ChatSuggestedAction;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;

/** Данные группы или события для открытия после аутентификации. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBootstrap {

    private long groupId;
    private String groupName;
    private GroupType groupType;
    private ChatSuggestedAction suggestedAction;
    private Long eventId;
}
