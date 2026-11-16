package skinemsya.vse.ru.api.internal.service.groups;

import java.util.List;

/** Проверка прав доступа к группе. */
public interface GroupAccessService {

    /** Требует, чтобы пользователь был участником группы. */
    void requireMember(long groupId, long userId);

    /** Требует, чтобы пользователь был владельцем группы. */
    void requireOwner(long groupId, long userId);

    /** Проверяет, состоит ли пользователь в группе. */
    boolean isMember(long groupId, long userId);

    /** Возвращает идентификаторы участников группы. */
    List<Long> memberUserIds(long groupId);
}
