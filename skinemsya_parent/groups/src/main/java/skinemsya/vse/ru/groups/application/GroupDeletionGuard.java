package skinemsya.vse.ru.groups.application;

public interface GroupDeletionGuard {

    void ensureGroupCanBeDeleted(long groupId);

    void prepareGroupForDeletion(long groupId);
}
