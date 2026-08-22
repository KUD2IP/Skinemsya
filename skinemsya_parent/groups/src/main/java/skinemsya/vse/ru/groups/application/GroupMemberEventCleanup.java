package skinemsya.vse.ru.groups.application;

public interface GroupMemberEventCleanup {

    void assertUserIsNotPayerOfActiveEvents(long groupId, long userId);

    void removeUserFromGroupEvents(long groupId, long userId);
}
