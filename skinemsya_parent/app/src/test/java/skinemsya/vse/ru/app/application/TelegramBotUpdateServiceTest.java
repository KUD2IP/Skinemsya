package skinemsya.vse.ru.app.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupType;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.application.TelegramGroupWelcomeService;
import skinemsya.vse.ru.integrations.domain.TelegramSentMessage;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.TelegramUserData;
import skinemsya.vse.ru.users.domain.User;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramBotUpdateServiceTest {

    @Mock
    private TelegramGroupWelcomeService groupWelcomeService;

    @Mock
    private TelegramBotClient telegramBotClient;

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private TelegramBotUpdateService updateService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDelegateWelcomeAndBootstrapGroupOnBotAdded() throws Exception {
        var update = objectMapper.readTree("""
                {
                  "my_chat_member": {
                    "chat": { "id": -100500, "title": "Weekend", "type": "supergroup" },
                    "from": { "id": 700001, "first_name": "Alice" },
                    "old_chat_member": { "status": "left" },
                    "new_chat_member": { "status": "member" }
                  }
                }
                """);

        when(userService.upsertFromTelegram(new TelegramUserData(700_001L, "Alice", "")))
                .thenReturn(user(1L));
        when(groupService.createFromChat(-100_500L, "Weekend", 1L))
                .thenReturn(group(10L, "Weekend"));

        updateService.handleUpdate(update);

        verify(groupWelcomeService).handleMyChatMemberUpdate(update);
        verify(groupService).createFromChat(-100_500L, "Weekend", 1L);
    }

    @Test
    void shouldJoinGroupOnStartCommandInGroupChat() throws Exception {
        var update = objectMapper.readTree("""
                {
                  "message": {
                    "chat": { "id": -100501, "title": "Trip", "type": "group" },
                    "from": { "id": 700002, "first_name": "Bob" },
                    "text": "/start"
                  }
                }
                """);

        when(userService.upsertFromTelegram(any(TelegramUserData.class))).thenReturn(user(2L));
        when(groupService.createFromChat(eq(-100_501L), eq("Trip"), eq(2L))).thenReturn(group(11L, "Trip"));
        when(telegramBotClient.sendMessageWithOpenAppButton(
                eq(-100_501L), any(), eq("Открыть Skinemsya"), eq("group")
        )).thenReturn(new TelegramSentMessage(1L));

        updateService.handleUpdate(update);

        verify(groupService).createFromChat(-100_501L, "Trip", 2L);
        verify(telegramBotClient).sendMessageWithOpenAppButton(
                eq(-100_501L), any(), eq("Открыть Skinemsya"), eq("group")
        );
    }

    @Test
    void shouldNotBootstrapGroupWhenBotLeavesChat() throws Exception {
        var update = objectMapper.readTree("""
                {
                  "my_chat_member": {
                    "chat": { "id": -100502, "title": "Trip", "type": "supergroup" },
                    "from": { "id": 700003, "first_name": "Carol" },
                    "old_chat_member": { "status": "member" },
                    "new_chat_member": { "status": "left" }
                  }
                }
                """);

        updateService.handleUpdate(update);

        verify(groupWelcomeService).handleMyChatMemberUpdate(update);
        verify(groupService, never()).createFromChat(anyLong(), any(), anyLong());
    }

    private static User user(long id) {
        return new User(id, 700_000L + id, "User " + id, null, Instant.now(), Instant.now());
    }

    private static Group group(long id, String name) {
        return new Group(id, name, GroupType.CHAT_LINKED, -100L, 1L, Instant.now(), Instant.now());
    }
}
