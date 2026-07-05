package skinemsya.vse.ru.groups.application;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.groups.domain.GroupRole;
import skinemsya.vse.ru.groups.domain.exception.GroupMemberAccessRequiredException;
import skinemsya.vse.ru.groups.domain.exception.GroupOwnerAccessRequiredException;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberRepository;

@Service
@Transactional(readOnly = true)
public class GroupAccessServiceImpl implements GroupAccessService {

    private final GroupMemberRepository groupMemberRepository;

    public GroupAccessServiceImpl(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    public void requireMember(long groupId, long userId) {
        if (!isMember(groupId, userId)) {
            throw new GroupMemberAccessRequiredException();
        }
    }

    @Override
    public void requireOwner(long groupId, long userId) {
        var member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupMemberAccessRequiredException::new);
        if (member.getRole() != GroupRole.OWNER) {
            throw new GroupOwnerAccessRequiredException();
        }
    }

    @Override
    public boolean isMember(long groupId, long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public List<Long> memberUserIds(long groupId) {
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(member -> member.getUserId())
                .toList();
    }
}
