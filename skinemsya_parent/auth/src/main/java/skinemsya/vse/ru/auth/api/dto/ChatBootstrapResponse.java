package skinemsya.vse.ru.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import skinemsya.vse.ru.auth.domain.ChatSuggestedAction;
import skinemsya.vse.ru.groups.domain.GroupType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatBootstrapResponse(
        long groupId,
        String groupName,
        GroupType groupType,
        ChatSuggestedAction suggestedAction,
        Long eventId
) {
}
