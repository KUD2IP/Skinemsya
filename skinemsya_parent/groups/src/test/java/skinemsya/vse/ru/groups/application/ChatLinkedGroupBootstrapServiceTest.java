package skinemsya.vse.ru.groups.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupRole;
import skinemsya.vse.ru.groups.domain.GroupType;
import skinemsya.vse.ru.groups.infrastructure.mapper.GroupMapper;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupEntity;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupMemberRepository;
import skinemsya.vse.ru.groups.infrastructure.persistence.GroupRepository;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.User;

@ExtendWith(MockitoExtension.class)
class ChatLinkedGroupBootstrapServiceTest {

    private static final long USER_ID = 1L;
    private static final long OWNER_ID = 9L;
    private static final long CHAT_ID = -1004350009778L;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private UserService userService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private ChatLinkedGroupBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService = new ChatLinkedGroupBootstrapService(
                groupRepository, groupMemberRepository, groupMapper, userService, eventPublisher);
        when(userService.findById(USER_ID))
                .thenReturn(Optional.of(new User(USER_ID, 100_001L, "User", null, Instant.now(), Instant.now())));
    }

    @Test
    void shouldCreateGroupWithOwnerWhenAbsent() {
        var group = chatLinkedGroupEntity(5L);
        var domainGroup = domainGroup(5L);

        when(groupRepository.insertChatLinkedGroupIfAbsent(
                        eq("Team chat"), eq(CHAT_ID), eq(USER_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        when(groupRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(group));
        when(groupMapper.toDomain(group)).thenReturn(domainGroup);

        var result = bootstrapService.joinOrCreateFromChat(CHAT_ID, "Team chat", USER_ID);

        assertThat(result.id()).isEqualTo(5L);
        verify(groupMemberRepository)
                .insertIfAbsent(eq(5L), eq(USER_ID), eq(GroupRole.OWNER.name()), any(Instant.class));
    }

    @Test
    void shouldJoinExistingGroupAsMemberWithoutErrors() {
        var group = chatLinkedGroupEntity(22L, OWNER_ID);
        var domainGroup = domainGroup(22L);

        when(groupRepository.insertChatLinkedGroupIfAbsent(
                        eq("Team chat"), eq(CHAT_ID), eq(USER_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(0);
        when(groupRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(group));
        when(groupMapper.toDomain(group)).thenReturn(domainGroup);

        var result = bootstrapService.joinOrCreateFromChat(CHAT_ID, "Team chat", USER_ID);

        assertThat(result.id()).isEqualTo(22L);
        verify(groupMemberRepository)
                .insertIfAbsent(eq(22L), eq(USER_ID), eq(GroupRole.MEMBER.name()), any(Instant.class));
    }

    @Test
    void shouldKeepExistingOwnerRoleWhenCreateRaceReportsConflict() {
        var group = chatLinkedGroupEntity(23L, USER_ID);
        var domainGroup = domainGroup(23L);

        when(groupRepository.insertChatLinkedGroupIfAbsent(
                        eq("Team chat"), eq(CHAT_ID), eq(USER_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(0);
        when(groupRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(group));
        when(groupMapper.toDomain(group)).thenReturn(domainGroup);

        bootstrapService.joinOrCreateFromChat(CHAT_ID, "Team chat", USER_ID);

        verify(groupMemberRepository)
                .insertIfAbsent(eq(23L), eq(USER_ID), eq(GroupRole.OWNER.name()), any(Instant.class));
    }

    private static GroupEntity chatLinkedGroupEntity(long id) {
        return chatLinkedGroupEntity(id, USER_ID);
    }

    private static GroupEntity chatLinkedGroupEntity(long id, long ownerId) {
        var entity = new GroupEntity();
        entity.setId(id);
        entity.setName("Team chat");
        entity.setType(GroupType.CHAT_LINKED);
        entity.setTelegramChatId(CHAT_ID);
        entity.setOwnerId(ownerId);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static Group domainGroup(long id) {
        return new Group(id, "Team chat", GroupType.CHAT_LINKED, CHAT_ID, USER_ID, Instant.now(), Instant.now());
    }
}
