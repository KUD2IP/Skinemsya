package skinemsya.vse.ru.impl.service.groups;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthGroup;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.service.auth.AuthGroupService;
import skinemsya.vse.ru.api.internal.service.groups.GroupService;

/** Адаптер операций с группами для модуля аутентификации. */
@Service
@RequiredArgsConstructor
public class AuthGroupServiceImpl implements AuthGroupService {

    private final GroupService groupService;

    @Override
    public void createFromChat(long chatId, String title, long userId) {
        groupService.createFromChat(chatId, title, userId);
    }

    @Override
    public Optional<Long> findIdByInviteCode(String inviteCode) {
        return groupService.findIdByInviteCode(inviteCode);
    }

    @Override
    public void joinFromInvite(long groupId, long userId) {
        groupService.joinFromInvite(groupId, userId);
    }

    @Override
    public Optional<AuthGroup> findById(long groupId) {
        return groupService.findById(groupId).map(AuthGroupServiceImpl::toAuthGroup);
    }

    @Override
    public Optional<AuthGroup> findByTelegramChatId(long chatId) {
        return groupService.findByTelegramChatId(chatId).map(AuthGroupServiceImpl::toAuthGroup);
    }

    private static AuthGroup toAuthGroup(Group group) {
        return new AuthGroup(group.getId(), group.getName(), group.getType());
    }
}
