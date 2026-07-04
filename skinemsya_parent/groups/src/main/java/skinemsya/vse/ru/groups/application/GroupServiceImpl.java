package skinemsya.vse.ru.groups.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.common.event.GroupMemberJoined;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupMember;
import skinemsya.vse.ru.groups.domain.GroupMemberView;
import skinemsya.vse.ru.groups.domain.GroupRole;
import skinemsya.vse.ru.groups.domain.GroupType;
import skinemsya.vse.ru.groups.domain.exception.GroupMemberAddFailedException;
import skinemsya.vse.ru.groups.domain.exception.GroupMemberAccessRequiredException;
import skinemsya.vse.ru.groups.domain.exception.GroupNameRequiredException;
import skinemsya.vse.ru.groups.domain.exception.GroupNameTooLongException;
import skinemsya.vse.ru.groups.domain.exception.GroupNotFoundException;
import skinemsya.vse.ru.groups.domain.exception.GroupOwnerAccessRequiredException;
import skinemsya.vse.ru.groups.domain.exception.GroupUserNotFoundException;
import skinemsya.vse.ru.groups.domain.exception.StandaloneGroupRequiredForManualMemberException;
import skinemsya.vse.ru.groups.domain.exception.UserNotFoundByTelegramIdException;
import skinemsya.vse.ru.groups.domain.exception.UserNotFoundByTelegramUsernameException;
import skinemsya.vse.ru.groups.infrastructure.mapper.GroupMapper;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupEntity;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberEntity;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberRepository;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupRepository;
import skinemsya.vse.ru.users.application.UserService;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMapper groupMapper;
    private final UserService userService;
    private final Optional<GroupDeletionGuard> groupDeletionGuard;
    private final ChatLinkedGroupBootstrapService chatLinkedGroupBootstrapService;
    private final ApplicationEventPublisher eventPublisher;

    public GroupServiceImpl(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupMapper groupMapper,
            UserService userService,
            Optional<GroupDeletionGuard> groupDeletionGuard,
            ChatLinkedGroupBootstrapService chatLinkedGroupBootstrapService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMapper = groupMapper;
        this.userService = userService;
        this.groupDeletionGuard = groupDeletionGuard;
        this.chatLinkedGroupBootstrapService = chatLinkedGroupBootstrapService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Group createStandalone(String name, long ownerUserId) {
        validateName(name);
        requireUserExists(ownerUserId);

        var now = Instant.now();
        var entity = new GroupEntity();
        entity.setName(name.trim());
        entity.setType(GroupType.STANDALONE);
        entity.setTelegramChatId(null);
        entity.setOwnerId(ownerUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity = groupRepository.save(entity);

        addMemberEntity(entity.getId(), ownerUserId, GroupRole.OWNER, now);
        return groupMapper.toDomain(entity);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Group createFromChat(long telegramChatId, String chatTitle, long userId) {
        return chatLinkedGroupBootstrapService.joinOrCreateFromChat(telegramChatId, chatTitle, userId);
    }

    @Override
    public void joinChatLinkedGroup(long groupId, long userId) {
        var group = getActiveGroup(groupId);
        if (group.getType() != GroupType.CHAT_LINKED) {
            return;
        }
        requireUserExists(userId);
        if (ensureMember(groupId, userId, GroupRole.MEMBER)) {
            eventPublisher.publishEvent(new GroupMemberJoined(groupId, userId));
        }
    }

    @Override
    public Group addMember(long groupId, long ownerUserId, long memberUserId) {
        var group = getActiveGroup(groupId);
        requireOwner(group, ownerUserId);
        if (group.getType() != GroupType.STANDALONE) {
            throw new StandaloneGroupRequiredForManualMemberException();
        }
        requireUserExists(memberUserId);
        if (ensureMember(groupId, memberUserId, GroupRole.MEMBER)) {
            eventPublisher.publishEvent(new GroupMemberJoined(groupId, memberUserId));
        }
        return groupMapper.toDomain(group);
    }

    @Override
    public GroupMember addMemberByTelegramUserId(long groupId, long ownerUserId, long telegramUserId) {
        var memberUserId = userService.findByTelegramUserId(telegramUserId)
                .orElseThrow(UserNotFoundByTelegramIdException::new)
                .id();
        addMember(groupId, ownerUserId, memberUserId);
        return groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId)
                .map(groupMapper::toDomain)
                .orElseThrow(GroupMemberAddFailedException::new);
    }

    @Override
    public GroupMemberView addMemberByTelegramUsername(long groupId, long ownerUserId, String telegramUsername) {
        var memberUserId = userService.findByTelegramUsername(telegramUsername)
                .orElseThrow(UserNotFoundByTelegramUsernameException::new)
                .id();
        addMember(groupId, ownerUserId, memberUserId);
        return toMemberView(groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(GroupMemberAddFailedException::new));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GroupMemberView> listMembers(long groupId, long userId, PageRequest pageRequest) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new GroupMemberAccessRequiredException();
        }
        var page = groupMemberRepository.findByGroupIdOrdered(
                groupId,
                GroupRole.OWNER,
                toPageable(pageRequest)
        );
        var items = page.getContent().stream().map(this::toMemberView).toList();
        return PageResult.of(items, pageRequest, page.getTotalElements());
    }

    private GroupMemberView toMemberView(GroupMemberEntity member) {
        var user = userService.findById(member.getUserId())
                .orElseThrow(GroupUserNotFoundException::new);
        return new GroupMemberView(
                member.getId(),
                member.getGroupId(),
                member.getUserId(),
                member.getRole(),
                user.displayName(),
                user.telegramUsername(),
                user.telegramUserId(),
                member.getJoinedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Group> findById(long groupId) {
        return groupRepository.findById(groupId).map(groupMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Group> listForUser(long userId, PageRequest pageRequest) {
        Page<GroupEntity> page = groupRepository.findAllByMemberUserId(
                userId,
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.page(),
                        pageRequest.size(),
                        Sort.by("name")
                )
        );
        var items = page.getContent().stream().map(groupMapper::toDomain).toList();
        return PageResult.of(items, pageRequest, page.getTotalElements());
    }

    @Override
    public Group updateName(long groupId, long ownerUserId, String name) {
        validateName(name);
        var entity = getActiveGroup(groupId);
        requireOwner(entity, ownerUserId);
        entity.setName(name.trim());
        entity.setUpdatedAt(Instant.now());
        return groupMapper.toDomain(groupRepository.save(entity));
    }

    @Override
    public void delete(long groupId, long ownerUserId) {
        var entity = getActiveGroup(groupId);
        requireOwner(entity, ownerUserId);
        groupDeletionGuard.ifPresent(guard -> {
            guard.ensureGroupCanBeDeleted(groupId);
            guard.prepareGroupForDeletion(groupId);
        });
        entity.setDeletedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        groupRepository.save(entity);
    }

    private GroupEntity getActiveGroup(long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);
    }

    private void requireOwner(GroupEntity group, long userId) {
        var member = groupMemberRepository.findByGroupIdAndUserId(group.getId(), userId)
                .orElseThrow(GroupMemberAccessRequiredException::new);
        if (member.getRole() != GroupRole.OWNER) {
            throw new GroupOwnerAccessRequiredException();
        }
    }

    private boolean ensureMember(long groupId, long userId, GroupRole roleForNewMember) {
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            return false;
        }
        groupMemberRepository.insertIfAbsent(groupId, userId, roleForNewMember.name(), Instant.now());
        return true;
    }

    private void addMemberEntity(long groupId, long userId, GroupRole role, Instant joinedAt) {
        var member = new GroupMemberEntity();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setJoinedAt(joinedAt);
        groupMemberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Group> findByTelegramChatId(long telegramChatId) {
        return groupRepository.findByTelegramChatId(telegramChatId).map(groupMapper::toDomain);
    }

    private void requireUserExists(long userId) {
        userService.findById(userId)
                .orElseThrow(GroupUserNotFoundException::new);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new GroupNameRequiredException();
        }
        if (name.trim().length() > 255) {
            throw new GroupNameTooLongException();
        }
    }

    private static org.springframework.data.domain.Pageable toPageable(PageRequest pageRequest) {
        return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
    }
}
