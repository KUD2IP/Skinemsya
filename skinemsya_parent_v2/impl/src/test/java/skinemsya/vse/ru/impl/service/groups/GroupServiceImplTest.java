package skinemsya.vse.ru.impl.service.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createGroup;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createGroupMember;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUser;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.pojo.groups.GroupMember;
import skinemsya.vse.ru.api.internal.repository.groups.GroupMemberRepository;
import skinemsya.vse.ru.api.internal.repository.groups.GroupRepository;
import skinemsya.vse.ru.api.internal.service.groups.GroupMemberEventCleanup;
import skinemsya.vse.ru.api.internal.service.users.UserService;

/** Тесты операций с группами. */
@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    private static final long OWNER_ID = 1L;
    private static final long MEMBER_ID = 2L;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserService userService;

    @Mock
    private ChatLinkedGroupBootstrapService chatLinkedGroupBootstrapService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GroupServiceImpl groupService;

    @Test
    void shouldCreateStandaloneGroupWithOwnerMember() {
        var savedGroup = createGroup();

        when(userService.findById(OWNER_ID)).thenReturn(Optional.of(createUser()));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);

        var result = groupService.createStandalone("Friends", OWNER_ID);

        assertThat(result.getType()).isEqualTo(GroupType.STANDALONE);
        assertThat(result.getName()).isEqualTo("Friends");

        var memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(GroupRole.OWNER);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    void shouldDelegateCreateFromChatToBootstrapService() {
        var chatGroup = chatLinkedGroup(5L, -100L);

        when(chatLinkedGroupBootstrapService.joinOrCreateFromChat(-100L, "Team chat", OWNER_ID))
                .thenReturn(chatGroup);

        var result = groupService.createFromChat(-100L, "Team chat", OWNER_ID);

        assertThat(result.getId()).isEqualTo(5L);
        verify(chatLinkedGroupBootstrapService).joinOrCreateFromChat(-100L, "Team chat", OWNER_ID);
    }

    @Test
    void shouldAddMemberToStandaloneGroupWhenOwner() {
        var group = createGroup();
        var ownerMember = createGroupMember();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(ownerMember));
        when(userService.findById(MEMBER_ID)).thenReturn(Optional.of(createUser()));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(false);

        groupService.addMember(10L, OWNER_ID, MEMBER_ID);

        verify(groupMemberRepository)
                .insertIfAbsent(eq(10L), eq(MEMBER_ID), eq(GroupRole.MEMBER), any(Instant.class));
    }

    @Test
    void shouldRejectAddMemberWhenNotOwner() {
        var group = createGroup();
        var member = member(10L, MEMBER_ID);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> groupService.addMember(10L, MEMBER_ID, OWNER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTHORIZATION_ERROR);
    }

    @Test
    void shouldRejectAddMemberToChatLinkedGroup() {
        var group = chatLinkedGroup(10L, -100L);
        var ownerMember = createGroupMember();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> groupService.addMember(10L, OWNER_ID, MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    @Test
    void shouldJoinFromInvite() {
        var group = createGroup();
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userService.findById(MEMBER_ID)).thenReturn(Optional.of(createUser()));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(false);

        groupService.joinFromInvite(10L, MEMBER_ID);

        verify(groupMemberRepository)
                .insertIfAbsent(eq(10L), eq(MEMBER_ID), eq(GroupRole.MEMBER), any(Instant.class));
    }

    @Test
    void shouldRemoveMemberWhenOwner() {
        var group = createGroup();
        var ownerMember = createGroupMember();
        var target = member(10L, MEMBER_ID);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(ownerMember));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(Optional.of(target));

        groupService.removeMember(10L, OWNER_ID, MEMBER_ID);

        verify(groupMemberRepository).delete(target);
    }

    @Test
    void shouldLeaveGroupAsMember() {
        var group = createGroup();
        var member = member(10L, MEMBER_ID);
        var cleanup = mock(GroupMemberEventCleanup.class);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(Optional.of(member));

        serviceWithCleanup(cleanup).leave(10L, MEMBER_ID);

        verify(cleanup).assertUserIsNotPayerOfActiveEvents(10L, MEMBER_ID);
        verify(cleanup).assertUserHasNoUnconfirmedTransfer(10L, MEMBER_ID);
        verify(cleanup).removeUserFromGroupEvents(10L, MEMBER_ID);
        verify(groupMemberRepository).delete(member);
    }

    @Test
    void shouldRejectLeaveWhenRequesterIsOwner() {
        var group = createGroup();
        var owner = createGroupMember();
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> groupService.leave(10L, OWNER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
        verify(groupMemberRepository, never()).delete(any());
    }

    @Test
    void shouldRejectLeaveWhenMemberIsEventPayer() {
        var group = createGroup();
        var member = member(10L, MEMBER_ID);
        var cleanup = mock(GroupMemberEventCleanup.class);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(Optional.of(member));
        doThrow(new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Member is a payer of active events"))
                .when(cleanup)
                .assertUserIsNotPayerOfActiveEvents(10L, MEMBER_ID);

        assertThatThrownBy(() -> serviceWithCleanup(cleanup).leave(10L, MEMBER_ID))
                .isInstanceOf(DomainException.class);
        verify(groupMemberRepository, never()).delete(any());
    }

    @Test
    void shouldRejectLeaveWhenMemberTransferIsUnconfirmed() {
        var group = createGroup();
        var member = member(10L, MEMBER_ID);
        var cleanup = mock(GroupMemberEventCleanup.class);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(Optional.of(member));
        doThrow(new DomainException(ErrorCode.DOMAIN_RULE_VIOLATION, "Member has open debts"))
                .when(cleanup)
                .assertUserHasNoUnconfirmedTransfer(10L, MEMBER_ID);

        assertThatThrownBy(() -> serviceWithCleanup(cleanup).leave(10L, MEMBER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
        verify(cleanup, never()).removeUserFromGroupEvents(10L, MEMBER_ID);
        verify(groupMemberRepository, never()).delete(any());
    }

    @Test
    void shouldRejectRemoveOwnerFromGroup() {
        var group = createGroup();
        var ownerMember = createGroupMember();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> groupService.removeMember(10L, OWNER_ID, OWNER_ID))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    @Test
    void shouldDeleteGroupWhenOwner() {
        var group = createGroup();
        var ownerMember = createGroupMember();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(ownerMember));

        groupService.delete(10L, OWNER_ID);

        verify(groupRepository).markDeleted(eq(10L), any(Instant.class), any(Instant.class));
    }

    private GroupServiceImpl serviceWithCleanup(GroupMemberEventCleanup cleanup) {
        return new GroupServiceImpl(
                groupRepository,
                groupMemberRepository,
                userService,
                Optional.empty(),
                Optional.of(cleanup),
                chatLinkedGroupBootstrapService,
                eventPublisher);
    }

    private static GroupMember member(long groupId, long userId) {
        return GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .role(GroupRole.MEMBER)
                .build();
    }

    private static Group chatLinkedGroup(long id, long chatId) {
        return Group.builder()
                .id(id)
                .name("Team chat")
                .type(GroupType.CHAT_LINKED)
                .telegramChatId(chatId)
                .ownerId(OWNER_ID)
                .build();
    }
}
