package skinemsya.vse.ru.impl.service.groups;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;
import skinemsya.vse.ru.api.internal.repository.groups.GroupMemberRepository;
import skinemsya.vse.ru.api.internal.service.groups.GroupAccessService;

/** Реализация проверок прав доступа к группе. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupAccessServiceImpl implements GroupAccessService {

    private final GroupMemberRepository groupMemberRepository;

    @Override
    public void requireMember(long groupId, long userId) {
        if (!isMember(groupId, userId)) {
            throw new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Group member access required");
        }
    }

    @Override
    public void requireOwner(long groupId, long userId) {
        var member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Group member access required"));
        if (member.getRole() != GroupRole.OWNER) {
            throw new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Group owner access required");
        }
    }

    @Override
    public boolean isMember(long groupId, long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public List<Long> memberUserIds(long groupId) {
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();
    }
}
