package skinemsya.vse.ru.impl.service.groups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUser;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import skinemsya.vse.ru.api.internal.enums.groups.GroupRole;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;
import skinemsya.vse.ru.api.internal.pojo.groups.Group;
import skinemsya.vse.ru.api.internal.repository.groups.GroupMemberRepository;
import skinemsya.vse.ru.api.internal.repository.groups.GroupRepository;
import skinemsya.vse.ru.api.internal.service.users.UserService;

/** Тесты создания и присоединения к группе, связанной с Telegram-чатом. */
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
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChatLinkedGroupBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService =
                new ChatLinkedGroupBootstrapService(groupRepository, groupMemberRepository, userService, eventPublisher);
        when(userService.findById(USER_ID)).thenReturn(Optional.of(createUser()));
    }

    @Test
    void shouldCreateGroupWithOwnerWhenAbsent() {
        var group = chatLinkedGroup(5L, USER_ID);

        when(groupRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(group));

        var result = bootstrapService.joinOrCreateFromChat(CHAT_ID, "Team chat", USER_ID);

        assertThat(result.getId()).isEqualTo(5L);
        verify(groupMemberRepository)
                .insertIfAbsent(eq(5L), eq(USER_ID), eq(GroupRole.OWNER), any(Instant.class));
    }

    @Test
    void shouldJoinExistingGroupAsMemberWithoutErrors() {
        var group = chatLinkedGroup(22L, OWNER_ID);

        when(groupRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(group));

        var result = bootstrapService.joinOrCreateFromChat(CHAT_ID, "Team chat", USER_ID);

        assertThat(result.getId()).isEqualTo(22L);
        verify(groupMemberRepository)
                .insertIfAbsent(eq(22L), eq(USER_ID), eq(GroupRole.MEMBER), any(Instant.class));
    }

    @Test
    void shouldKeepExistingOwnerRoleWhenCreateRaceReportsConflict() {
        var group = chatLinkedGroup(23L, USER_ID);

        when(groupRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(group));

        bootstrapService.joinOrCreateFromChat(CHAT_ID, "Team chat", USER_ID);

        verify(groupMemberRepository)
                .insertIfAbsent(eq(23L), eq(USER_ID), eq(GroupRole.OWNER), any(Instant.class));
    }

    private static Group chatLinkedGroup(long id, long ownerId) {
        return Group.builder()
                .id(id)
                .name("Team chat")
                .type(GroupType.CHAT_LINKED)
                .telegramChatId(CHAT_ID)
                .ownerId(ownerId)
                .build();
    }
}
