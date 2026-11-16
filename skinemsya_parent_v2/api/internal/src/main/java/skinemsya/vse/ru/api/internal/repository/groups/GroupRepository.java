package skinemsya.vse.ru.api.internal.repository.groups;

import java.time.Instant;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;

/** Порт хранения групп. */
public interface GroupRepository {

    /** Находит активную группу по идентификатору. */
    Optional<Group> findById(long groupId);

    /** Находит активную группу по идентификатору Telegram-чата. */
    Optional<Group> findByTelegramChatId(long telegramChatId);

    /** Находит активную группу по коду приглашения. */
    Optional<Group> findByInviteCode(String inviteCode);

    /** Возвращает страницу групп, в которых состоит пользователь. */
    PageResult<Group> findAllByMemberUserId(long userId, PageRequest pageRequest);

    /** Сохраняет группу. */
    Group save(Group group);

    /** Атомарно создаёт группу, связанную с Telegram-чатом, если её ещё нет. */
    void insertChatLinkedGroupIfAbsent(
            String name, long telegramChatId, long ownerId, String inviteCode, Instant createdAt, Instant updatedAt);

    /** Помечает группу удалённой. */
    void markDeleted(long groupId, Instant deletedAt, Instant updatedAt);
}
