package skinemsya.vse.ru.api.internal.repository.groups;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;

/** Порт хранения участников групп. */
public interface GroupMemberRepository {

    /** Находит участника по группе и пользователю. */
    Optional<GroupMember> findByGroupIdAndUserId(long groupId, long userId);

    /** Возвращает участников группы. */
    List<GroupMember> findByGroupId(long groupId);

    /** Проверяет, состоит ли пользователь в группе. */
    boolean existsByGroupIdAndUserId(long groupId, long userId);

    /** Возвращает страницу участников группы: сначала владелец, затем по дате вступления. */
    PageResult<GroupMember> findByGroupIdOrdered(long groupId, PageRequest pageRequest);

    /** Сохраняет участника группы. */
    GroupMember save(GroupMember member);

    /** Атомарно добавляет участника, если его ещё нет в группе. */
    void insertIfAbsent(long groupId, long userId, GroupRole role, Instant joinedAt);

    /** Удаляет участника группы. */
    void delete(GroupMember member);
}
