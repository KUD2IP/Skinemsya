package skinemsya.vse.ru.groups.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.groups.domain.exception.GroupOwnerAccessRequiredException;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupRole;
import skinemsya.vse.ru.groups.domain.GroupType;
import skinemsya.vse.ru.groups.infrastructure.mapper.GroupMapper;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupEntity;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberEntity;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberRepository;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupRepository;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.User;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private UserService userService;

    @Mock
    private ChatLinkedGroupBootstrapService chatLinkedGroupBootstrapService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GroupServiceImpl groupService;

    private static final long OWNER_ID = 1L;
    private static final long MEMBER_ID = 2L;

    @Test
    void shouldCreateStandaloneGroupWithOwnerMember() {
        var savedGroup = standaloneGroupEntity(10L);
        var domainGroup = domainGroup(10L, GroupType.STANDALONE, null);

        when(userService.findById(OWNER_ID)).thenReturn(Optional.of(user(OWNER_ID)));

        when(groupRepository.save(any(GroupEntity.class))).thenReturn(savedGroup);
        when(groupMapper.toDomain(savedGroup)).thenReturn(domainGroup);

        var result = groupService.createStandalone("Friends", OWNER_ID);

        assertThat(result.type()).isEqualTo(GroupType.STANDALONE);
        assertThat(result.name()).isEqualTo("Friends");

        var memberCaptor = ArgumentCaptor.forClass(GroupMemberEntity.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(GroupRole.OWNER);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(OWNER_ID);
    }

    @Test
    void shouldDelegateCreateFromChatToBootstrapService() {
        var domainGroup = domainGroup(5L, GroupType.CHAT_LINKED, -100L);

        when(chatLinkedGroupBootstrapService.joinOrCreateFromChat(-100L, "Team chat", OWNER_ID))
                .thenReturn(domainGroup);

        var result = groupService.createFromChat(-100L, "Team chat", OWNER_ID);

        assertThat(result.id()).isEqualTo(5L);
        verify(chatLinkedGroupBootstrapService).joinOrCreateFromChat(-100L, "Team chat", OWNER_ID);
    }

    @Test
    void shouldAddMemberToStandaloneGroupWhenOwner() {
        var group = standaloneGroupEntity(10L);
        var ownerMember = ownerMemberEntity(10L, OWNER_ID);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, OWNER_ID)).thenReturn(Optional.of(ownerMember));
        when(userService.findById(MEMBER_ID)).thenReturn(Optional.of(user(MEMBER_ID)));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(false);
        when(groupMapper.toDomain(group)).thenReturn(domainGroup(10L, GroupType.STANDALONE, null));

        groupService.addMember(10L, OWNER_ID, MEMBER_ID);

        verify(groupMemberRepository).insertIfAbsent(eq(10L), eq(MEMBER_ID), eq(GroupRole.MEMBER.name()), any(Instant.class));
    }

    @Test
    void shouldRejectAddMemberWhenNotOwner() {
        var group = standaloneGroupEntity(10L);
        var member = new GroupMemberEntity();
        member.setRole(GroupRole.MEMBER);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(10L, MEMBER_ID)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> groupService.addMember(10L, MEMBER_ID, OWNER_ID))
                .isInstanceOf(GroupOwnerAccessRequiredException.class)
                .extracting(ex -> ((GroupOwnerAccessRequiredException) ex).errorCode())
                .isEqualTo(ErrorCode.AUTHORIZATION_ERROR);
    }

    private static User user(long id) {
        return new User(id, 100_000L + id, "User " + id, null, Instant.now(), Instant.now());
    }

    private static GroupEntity standaloneGroupEntity(long id) {
        var entity = new GroupEntity();
        entity.setId(id);
        entity.setName("Friends");
        entity.setType(GroupType.STANDALONE);
        entity.setOwnerId(OWNER_ID);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static GroupEntity chatLinkedGroupEntity(long id, long chatId) {
        var entity = new GroupEntity();
        entity.setId(id);
        entity.setName("Team chat");
        entity.setType(GroupType.CHAT_LINKED);
        entity.setTelegramChatId(chatId);
        entity.setOwnerId(OWNER_ID);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static GroupMemberEntity ownerMemberEntity(long groupId, long userId) {
        var member = new GroupMemberEntity();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(GroupRole.OWNER);
        return member;
    }

    private static Group domainGroup(long id, GroupType type, Long chatId) {
        return new Group(id, "Friends", type, chatId, OWNER_ID, Instant.now(), Instant.now());
    }
}
