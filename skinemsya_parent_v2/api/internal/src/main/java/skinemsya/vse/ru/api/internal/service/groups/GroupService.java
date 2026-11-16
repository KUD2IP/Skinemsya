package skinemsya.vse.ru.api.internal.service.groups;

import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMemberView;

/** Операции с группами и их участниками. */
public interface GroupService {

    /** Создаёт автономную группу с владельцем. */
    Group createStandalone(String name, long ownerUserId);

    /** Создаёт или находит группу, связанную с Telegram-чатом. */
    Group createFromChat(long telegramChatId, String chatTitle, long userId);

    /** Добавляет пользователя в группу, связанную с чатом. */
    void joinChatLinkedGroup(long groupId, long userId);

    /** Добавляет пользователя в группу по приглашению. */
    void joinFromInvite(long groupId, long userId);

    /** Возвращает код приглашения группы. */
    String inviteCode(long groupId);

    /** Находит идентификатор группы по коду приглашения. */
    Optional<Long> findIdByInviteCode(String inviteCode);

    /** Добавляет участника в автономную группу от имени владельца. */
    Group addMember(long groupId, long ownerUserId, long memberUserId);

    /** Добавляет участника по идентификатору Telegram. */
    GroupMember addMemberByTelegramUserId(long groupId, long ownerUserId, long telegramUserId);

    /** Добавляет участника по имени в Telegram. */
    GroupMemberView addMemberByTelegramUsername(long groupId, long ownerUserId, String telegramUsername);

    /** Возвращает страницу участников группы. */
    PageResult<GroupMemberView> listMembers(long groupId, long userId, PageRequest pageRequest);

    /** Находит группу по идентификатору. */
    Optional<Group> findById(long groupId);

    /** Находит группу по идентификатору Telegram-чата. */
    Optional<Group> findByTelegramChatId(long telegramChatId);

    /** Возвращает страницу групп пользователя. */
    PageResult<Group> listForUser(long userId, PageRequest pageRequest);

    /** Переименовывает группу от имени владельца. */
    Group updateName(long groupId, long ownerUserId, String name);

    /** Удаляет участника из группы от имени владельца. */
    void removeMember(long groupId, long ownerUserId, long memberUserId);

    /** Выводит пользователя из группы. */
    void leave(long groupId, long userId);

    /** Помечает группу удалённой от имени владельца. */
    void delete(long groupId, long ownerUserId);
}
