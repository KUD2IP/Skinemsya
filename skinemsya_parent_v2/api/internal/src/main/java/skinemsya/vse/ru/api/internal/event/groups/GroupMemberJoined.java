package skinemsya.vse.ru.api.internal.event.groups;

/** Событие вступления пользователя в группу. */
public record GroupMemberJoined(long groupId, long userId) {}
