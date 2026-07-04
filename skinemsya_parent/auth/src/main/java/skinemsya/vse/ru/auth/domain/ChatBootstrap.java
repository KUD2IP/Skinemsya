package skinemsya.vse.ru.auth.domain;

import skinemsya.vse.ru.groups.domain.GroupType;

public record ChatBootstrap(
        long groupId,
        String groupName,
        GroupType groupType,
        ChatSuggestedAction suggestedAction
) {
}
