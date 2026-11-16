package skinemsya.vse.ru.api.internal.service.groups;

/** Защита группы от удаления при наличии блокирующих событий. */
public interface GroupDeletionGuard {

    /** Проверяет, что группу можно удалить. */
    void ensureGroupCanBeDeleted(long groupId);

    /** Подготавливает связанные данные к удалению группы. */
    void prepareGroupForDeletion(long groupId);
}
