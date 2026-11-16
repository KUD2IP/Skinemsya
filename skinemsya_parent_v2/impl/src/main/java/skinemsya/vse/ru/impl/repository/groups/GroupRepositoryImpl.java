package skinemsya.vse.ru.impl.repository.groups;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.repository.groups.GroupRepository;
import skinemsya.vse.ru.data.entity.groups.GroupEntity;
import skinemsya.vse.ru.impl.mapper.entity.GroupEntityMapper;

/** Адаптер между внутренним портом групп и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupJpaRepository groupJpaRepository;
    private final GroupEntityMapper groupEntityMapper;

    @Override
    public Optional<Group> findById(long groupId) {
        return groupJpaRepository.findById(groupId).map(groupEntityMapper::toGroup);
    }

    @Override
    public Optional<Group> findByTelegramChatId(long telegramChatId) {
        return groupJpaRepository.findByTelegramChatId(telegramChatId).map(groupEntityMapper::toGroup);
    }

    @Override
    public Optional<Group> findByInviteCode(String inviteCode) {
        return groupJpaRepository.findByInviteCode(inviteCode).map(groupEntityMapper::toGroup);
    }

    @Override
    public PageResult<Group> findAllByMemberUserId(long userId, PageRequest pageRequest) {
        Page<GroupEntity> page = groupJpaRepository.findAllByMemberUserId(
                userId, org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(), Sort.by("name")));
        return new PageResult<>(
                page.getContent().stream().map(groupEntityMapper::toGroup).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Override
    public Group save(Group group) {
        var saved = groupJpaRepository.save(groupEntityMapper.toEntity(group));
        return groupEntityMapper.toGroup(saved);
    }

    @Override
    public void insertChatLinkedGroupIfAbsent(
            String name, long telegramChatId, long ownerId, String inviteCode, Instant createdAt, Instant updatedAt) {
        groupJpaRepository.insertChatLinkedGroupIfAbsent(name, telegramChatId, ownerId, inviteCode, createdAt, updatedAt);
    }

    @Override
    public void markDeleted(long groupId, Instant deletedAt, Instant updatedAt) {
        groupJpaRepository.markDeleted(groupId, deletedAt, updatedAt);
    }
}
