package skinemsya.vse.ru.impl.repository.groups;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;
import skinemsya.vse.ru.api.internal.repository.groups.GroupMemberRepository;
import skinemsya.vse.ru.data.entity.groups.GroupMemberEntity;
import skinemsya.vse.ru.impl.mapper.entity.GroupEntityMapper;

/** Адаптер между внутренним портом участников групп и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class GroupMemberRepositoryImpl implements GroupMemberRepository {

    private final GroupMemberJpaRepository groupMemberJpaRepository;
    private final GroupEntityMapper groupEntityMapper;

    @Override
    public Optional<GroupMember> findByGroupIdAndUserId(long groupId, long userId) {
        return groupMemberJpaRepository.findByGroupIdAndUserId(groupId, userId).map(groupEntityMapper::toGroupMember);
    }

    @Override
    public List<GroupMember> findByGroupId(long groupId) {
        return groupMemberJpaRepository.findByGroupId(groupId).stream()
                .map(groupEntityMapper::toGroupMember)
                .toList();
    }

    @Override
    public boolean existsByGroupIdAndUserId(long groupId, long userId) {
        return groupMemberJpaRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Override
    public PageResult<GroupMember> findByGroupIdOrdered(
            long groupId, PageRequest pageRequest) {
        Page<GroupMemberEntity> page = groupMemberJpaRepository.findByGroupIdOrdered(
                groupId, GroupRole.OWNER, org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size()));
        return new PageResult<>(
                page.getContent().stream().map(groupEntityMapper::toGroupMember).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Override
    public GroupMember save(GroupMember member) {
        var saved = groupMemberJpaRepository.save(groupEntityMapper.toEntity(member));
        return groupEntityMapper.toGroupMember(saved);
    }

    @Override
    public void insertIfAbsent(long groupId, long userId, GroupRole role, Instant joinedAt) {
        groupMemberJpaRepository.insertIfAbsent(groupId, userId, role.name(), joinedAt);
    }

    @Override
    public void delete(GroupMember member) {
        groupMemberJpaRepository.delete(groupEntityMapper.toEntity(member));
    }
}
