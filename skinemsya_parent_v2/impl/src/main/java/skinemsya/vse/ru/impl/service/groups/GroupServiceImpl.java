package skinemsya.vse.ru.impl.service.groups;

import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;
import skinemsya.vse.ru.api.internal.event.groups.GroupMemberJoined;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMemberView;
import skinemsya.vse.ru.api.internal.repository.groups.GroupMemberRepository;
import skinemsya.vse.ru.api.internal.repository.groups.GroupRepository;
import skinemsya.vse.ru.api.internal.service.groups.GroupDeletionGuard;
import skinemsya.vse.ru.api.internal.service.groups.GroupMemberEventCleanup;
import skinemsya.vse.ru.api.internal.service.groups.GroupService;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.impl.support.util.InviteCodes;

/** Реализация операций с группами поверх внутренних портов. */
@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserService userService;
    private final Optional<GroupDeletionGuard> groupDeletionGuard;
    private final Optional<GroupMemberEventCleanup> groupMemberEventCleanup;
    private final ChatLinkedGroupBootstrapService chatLinkedGroupBootstrapService;
    private final ApplicationEventPublisher eventPublisher;

    public GroupServiceImpl(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            UserService userService,
            Optional<GroupDeletionGuard> groupDeletionGuard,
            Optional<GroupMemberEventCleanup> groupMemberEventCleanup,
            ChatLinkedGroupBootstrapService chatLinkedGroupBootstrapService,
            ApplicationEventPublisher eventPublisher) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userService = userService;
        this.groupDeletionGuard = groupDeletionGuard == null ? Optional.empty() : groupDeletionGuard;
        this.groupMemberEventCleanup = groupMemberEventCleanup == null ? Optional.empty() : groupMemberEventCleanup;
        this.chatLinkedGroupBootstrapService = chatLinkedGroupBootstrapService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Group createStandalone(String name, long ownerUserId) {
        validateName(name);
        requireUserExists(ownerUserId);

        var now = Instant.now();
        var group = Group.builder()
                .name(name.trim())
                .type(GroupType.STANDALONE)
                .ownerId(ownerUserId)
                .inviteCode(InviteCodes.random())
                .createdAt(now)
                .updatedAt(now)
                .build();
        group = groupRepository.save(group);

        groupMemberRepository.save(GroupMember.builder()
                .groupId(group.getId())
                .userId(ownerUserId)
                .role(GroupRole.OWNER)
                .joinedAt(now)
                .build());
        return group;
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
    public void joinFromInvite(long groupId, long userId) {
        getActiveGroup(groupId);
        requireUserExists(userId);
        if (ensureMember(groupId, userId, GroupRole.MEMBER)) {
            eventPublisher.publishEvent(new GroupMemberJoined(groupId, userId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String inviteCode(long groupId) {
        return getActiveGroup(groupId).getInviteCode();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findIdByInviteCode(String inviteCode) {
        if (!InviteCodes.isToken(inviteCode)) {
            return Optional.empty();
        }
        return groupRepository.findByInviteCode(inviteCode).map(Group::getId);
    }

    @Override
    public Group addMember(long groupId, long ownerUserId, long memberUserId) {
        var group = getActiveGroup(groupId);
        requireOwner(group, ownerUserId);
        if (group.getType() != GroupType.STANDALONE) {
            throw new DomainException(
                    ErrorCode.DOMAIN_RULE_VIOLATION, "Manual member add is allowed only for standalone groups");
        }
        requireUserExists(memberUserId);
        if (ensureMember(groupId, memberUserId, GroupRole.MEMBER)) {
            eventPublisher.publishEvent(new GroupMemberJoined(groupId, memberUserId));
        }
        return group;
    }

    @Override
    public GroupMember addMemberByTelegramUserId(long groupId, long ownerUserId, long telegramUserId) {
        var memberUserId = userService
                .findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found by Telegram id"))
                .getId();
        addMember(groupId, ownerUserId, memberUserId);
        return groupMemberRepository
                .findByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new DomainException(ErrorCode.DOMAIN_CONFLICT, "Group member add failed"));
    }

    @Override
    public GroupMemberView addMemberByTelegramUsername(long groupId, long ownerUserId, String telegramUsername) {
        var memberUserId = userService
                .findByTelegramUsername(telegramUsername)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found by Telegram username"))
                .getId();
        addMember(groupId, ownerUserId, memberUserId);
        return toMemberView(groupMemberRepository
                .findByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new DomainException(ErrorCode.DOMAIN_CONFLICT, "Group member add failed")));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GroupMemberView> listMembers(long groupId, long userId, PageRequest pageRequest) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Group member access required");
        }
        var page = groupMemberRepository.findByGroupIdOrdered(groupId, pageRequest);
        var items = page.items().stream().map(this::toMemberView).toList();
        return PageResult.of(items, pageRequest, page.totalElements());
    }

    private GroupMemberView toMemberView(GroupMember member) {
        var user = userService
                .findById(member.getUserId())
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group user not found"));
        return GroupMemberView.builder()
                .id(member.getId())
                .groupId(member.getGroupId())
                .userId(member.getUserId())
                .role(member.getRole())
                .displayName(user.getDisplayName())
                .telegramUsername(user.getTelegramUsername())
                .telegramUserId(user.getTelegramUserId())
                .joinedAt(member.getJoinedAt())
                .photoUrl(user.getPhotoUrl())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Group> findById(long groupId) {
        return groupRepository.findById(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Group> listForUser(long userId, PageRequest pageRequest) {
        return groupRepository.findAllByMemberUserId(userId, pageRequest);
    }

    @Override
    public Group updateName(long groupId, long ownerUserId, String name) {
        validateName(name);
        var group = getActiveGroup(groupId);
        requireOwner(group, ownerUserId);
        group.setName(name.trim());
        group.setUpdatedAt(Instant.now());
        return groupRepository.save(group);
    }

    @Override
    public void removeMember(long groupId, long ownerUserId, long memberUserId) {
        var group = getActiveGroup(groupId);
        requireOwner(group, ownerUserId);
        if (memberUserId == group.getOwnerId()) {
            throw new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Group owner cannot be removed");
        }
        var member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group member not found"));
        groupMemberEventCleanup.ifPresent(cleanup -> {
            cleanup.assertUserIsNotPayerOfActiveEvents(groupId, memberUserId);
            cleanup.removeUserFromGroupEvents(groupId, memberUserId);
        });
        groupMemberRepository.delete(member);
    }

    @Override
    public void leave(long groupId, long userId) {
        var group = getActiveGroup(groupId);
        var member = groupMemberRepository
                .findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group member not found"));
        if (userId == group.getOwnerId() || member.getRole() == GroupRole.OWNER) {
            throw new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Group owner cannot leave the group");
        }
        groupMemberEventCleanup.ifPresent(cleanup -> {
            cleanup.assertUserIsNotPayerOfActiveEvents(groupId, userId);
            cleanup.assertUserHasNoUnconfirmedTransfer(groupId, userId);
            cleanup.removeUserFromGroupEvents(groupId, userId);
        });
        groupMemberRepository.delete(member);
    }

    @Override
    public void delete(long groupId, long ownerUserId) {
        var group = getActiveGroup(groupId);
        requireOwner(group, ownerUserId);
        groupDeletionGuard.ifPresent(guard -> {
            guard.ensureGroupCanBeDeleted(groupId);
            guard.prepareGroupForDeletion(groupId);
        });
        var now = Instant.now();
        groupRepository.markDeleted(groupId, now, now);
    }

    private Group getActiveGroup(long groupId) {
        return groupRepository
                .findById(groupId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group not found"));
    }

    private void requireOwner(Group group, long userId) {
        var member = groupMemberRepository
                .findByGroupIdAndUserId(group.getId(), userId)
                .orElseThrow(() -> new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Group member access required"));
        if (member.getRole() != GroupRole.OWNER) {
            throw new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Group owner access required");
        }
    }

    private boolean ensureMember(long groupId, long userId, GroupRole roleForNewMember) {
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            return false;
        }
        groupMemberRepository.insertIfAbsent(groupId, userId, roleForNewMember, Instant.now());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Group> findByTelegramChatId(long telegramChatId) {
        return groupRepository.findByTelegramChatId(telegramChatId);
    }

    private void requireUserExists(long userId) {
        userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "Group user not found"));
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Group name is required");
        }
        if (name.trim().length() > 255) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Group name is too long");
        }
    }
}
