package skinemsya.vse.ru.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        var update = objectMapper.readTree(
                """
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
        when(groupService.createFromChat(-100_500L, "Weekend", 1L)).thenReturn(group(10L, "Weekend"));

        updateService.handleUpdate(update);

        verify(groupWelcomeService).handleMyChatMemberUpdate(update);
        verify(groupService).createFromChat(-100_500L, "Weekend", 1L);
    }

    @Test
    void shouldJoinGroupOnStartCommandInGroupChat() throws Exception {
        var update = objectMapper.readTree(
                """
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
        when(telegramBotClient.sendMessageWithOpenAppButton(eq(-100_501L), any(), eq("Открыть Skinemsya"), eq("group")))
                .thenReturn(new TelegramSentMessage(1L));

        updateService.handleUpdate(update);

        verify(groupService).createFromChat(-100_501L, "Trip", 2L);
        assertThat(capturedReply(-100_501L, "group")).contains("Вы в группе этого чата");
    }

    @Test
    void shouldSendPrivateStartHintWithoutBootstrappingGroup() throws Exception {
        var update = objectMapper.readTree(
                """
                {
                  "message": {
                    "chat": { "id": 700010, "type": "private" },
                    "from": { "id": 700010, "first_name": "Dana" },
                    "text": "/start"
                  }
                }
                """);

        when(telegramBotClient.sendMessageWithOpenAppButton(
                        eq(700_010L), any(), eq("Открыть Skinemsya"), eq("private")))
                .thenReturn(new TelegramSentMessage(2L));

        updateService.handleUpdate(update);

        verify(groupService, never()).createFromChat(anyLong(), any(), anyLong());
        String reply = capturedReply(700_010L, "private");
        assertThat(reply).contains("Привет! Это");
        assertThat(reply).contains("Как начать");
        assertThat(reply).doesNotContain("личные уведомления");
    }

    @Test
    void shouldSendPrivateHelpWithoutBootstrappingGroup() throws Exception {
        stubOpenAppButton();
        updateService.handleUpdate(commandUpdate(700_011L, "private", "/help"));

        verify(groupService, never()).createFromChat(anyLong(), any(), anyLong());
        String reply = capturedReply(700_011L, "private");
        assertThat(reply).contains("Как пользоваться");
        assertThat(reply).contains("/open");
        assertThat(reply).contains("Добавьте бота в групповой чат");
    }

    @Test
    void shouldSendGroupHelpWithoutBootstrappingGroup() throws Exception {
        stubOpenAppButton();
        updateService.handleUpdate(commandUpdate(-100_503L, "group", "/help@skinemsyabot"));

        verify(groupService, never()).createFromChat(anyLong(), any(), anyLong());
        String reply = capturedReply(-100_503L, "group");
        assertThat(reply).contains("в этом чате");
        assertThat(reply).doesNotContain("Добавьте бота в групповой чат");
    }

    @Test
    void shouldSendPrivateOpenWithoutBootstrappingGroup() throws Exception {
        stubOpenAppButton();
        updateService.handleUpdate(commandUpdate(700_012L, "private", "/open"));

        verify(groupService, never()).createFromChat(anyLong(), any(), anyLong());
        assertThat(capturedReply(700_012L, "private")).contains("Откройте Skinemsya, чтобы делить расходы");
    }

    @Test
    void shouldSendGroupOpenWithoutBootstrappingGroup() throws Exception {
        stubOpenAppButton();
        updateService.handleUpdate(commandUpdate(-100_504L, "supergroup", "/open"));

        verify(groupService, never()).createFromChat(anyLong(), any(), anyLong());
        assertThat(capturedReply(-100_504L, "supergroup")).contains("Откройте Skinemsya, чтобы делить расходы");
    }

    @Test
    void shouldNotBootstrapGroupWhenBotLeavesChat() throws Exception {
        var update = objectMapper.readTree(
                """
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

    private void stubOpenAppButton() {
        when(telegramBotClient.sendMessageWithOpenAppButton(anyLong(), any(), eq("Открыть Skinemsya"), any()))
                .thenReturn(new TelegramSentMessage(1L));
    }

    private String capturedReply(long chatId, String chatType) {
        var text = ArgumentCaptor.forClass(String.class);
        verify(telegramBotClient)
                .sendMessageWithOpenAppButton(eq(chatId), text.capture(), eq("Открыть Skinemsya"), eq(chatType));
        return text.getValue();
    }

    private com.fasterxml.jackson.databind.JsonNode commandUpdate(long chatId, String chatType, String text)
            throws Exception {
        boolean group = "group".equals(chatType) || "supergroup".equals(chatType);
        String titleField = group ? ", \"title\": \"Trip\"" : "";
        return objectMapper.readTree(
                """
                {
                  "message": {
                    "chat": { "id": %d, "type": "%s"%s },
                    "from": { "id": 700002, "first_name": "Bob" },
                    "text": "%s"
                  }
                }
                """
                        .formatted(chatId, chatType, titleField, text));
    }

    private static User user(long id) {
        return new User(id, 700_000L + id, "User " + id, null, Instant.now(), Instant.now());
    }

    private static Group group(long id, String name) {
        return new Group(id, name, GroupType.CHAT_LINKED, -100L, 1L, Instant.now(), Instant.now());
    }
}
