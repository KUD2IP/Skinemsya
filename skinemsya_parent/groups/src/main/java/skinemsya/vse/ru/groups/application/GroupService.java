package skinemsya.vse.ru.groups.application;

import java.util.Optional;
import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupMember;
import skinemsya.vse.ru.groups.domain.GroupMemberView;

public interface GroupService {

    Group createStandalone(String name, long ownerUserId);

    Group createFromChat(long telegramChatId, String chatTitle, long userId);

    void joinChatLinkedGroup(long groupId, long userId);

    Group addMember(long groupId, long ownerUserId, long memberUserId);

    GroupMember addMemberByTelegramUserId(long groupId, long ownerUserId, long telegramUserId);

    GroupMemberView addMemberByTelegramUsername(long groupId, long ownerUserId, String telegramUsername);

    PageResult<GroupMemberView> listMembers(long groupId, long userId, PageRequest pageRequest);

    Optional<Group> findById(long groupId);

    Optional<Group> findByTelegramChatId(long telegramChatId);

    PageResult<Group> listForUser(long userId, PageRequest pageRequest);

    Group updateName(long groupId, long ownerUserId, String name);

    void delete(long groupId, long ownerUserId);
}
