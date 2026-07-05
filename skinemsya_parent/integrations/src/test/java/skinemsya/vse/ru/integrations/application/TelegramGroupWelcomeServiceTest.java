package skinemsya.vse.ru.integrations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import skinemsya.vse.ru.integrations.domain.TelegramSentMessage;

class TelegramGroupWelcomeServiceTest {

    private RecordingBotClient botClient;
    private TelegramGroupWelcomeService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        botClient = new RecordingBotClient();
        service = new TelegramGroupWelcomeService(botClient);
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldSendWelcomeAndPinWhenBotAddedToGroup() {
        service.handleMyChatMemberUpdate(botJoinedUpdate("supergroup", "left", "administrator"));

        assertThat(botClient.htmlMessages).hasSize(1);
        assertThat(botClient.htmlMessages.getFirst()).contains("Добро пожаловать");
        assertThat(botClient.htmlMessages.getFirst()).contains("@ivan");
        assertThat(botClient.buttonMessages).hasSize(1);
        assertThat(botClient.pinnedMessageIds).containsExactly(42L);
    }

    @Test
    void shouldPinButtonWhenBotPromotedToAdmin() {
        service.handleMyChatMemberUpdate(botJoinedUpdate("supergroup", "member", "administrator"));

        assertThat(botClient.htmlMessages).isEmpty();
        assertThat(botClient.buttonMessages).hasSize(1);
        assertThat(botClient.pinnedMessageIds).containsExactly(42L);
    }

    @Test
    void shouldRetryPinWithoutSendingDuplicateButtonAfterPromotion() {
        botClient.failNextPin = true;

        service.handleMyChatMemberUpdate(botJoinedUpdate("supergroup", "left", "member"));
        service.handleMyChatMemberUpdate(botJoinedUpdate("supergroup", "member", "administrator"));

        assertThat(botClient.buttonMessages).hasSize(1);
        assertThat(botClient.pinnedMessageIds).containsExactly(42L);
    }

    @Test
    void shouldIgnorePrivateChat() {
        service.handleMyChatMemberUpdate(botJoinedUpdate("private", "left", "member"));

        assertThat(botClient.htmlMessages).isEmpty();
    }

    private ObjectNode botJoinedUpdate(String chatType, String oldStatus, String newStatus) {
        ObjectNode update = objectMapper.createObjectNode();
        ObjectNode event = update.putObject("my_chat_member");
        ObjectNode chat = event.putObject("chat");
        chat.put("id", -100123L);
        chat.put("type", chatType);
        chat.put("title", "Trip");
        ObjectNode from = event.putObject("from");
        from.put("id", 1);
        from.put("username", "ivan");
        event.putObject("old_chat_member").put("status", oldStatus);
        event.putObject("new_chat_member").put("status", newStatus);
        return update;
    }

    private static final class RecordingBotClient implements TelegramBotClient {

        private final List<String> htmlMessages = new ArrayList<>();
        private final List<String> buttonMessages = new ArrayList<>();
        private final List<Long> pinnedMessageIds = new ArrayList<>();
        private boolean failNextPin;

        @Override
        public void initialize() {}

        @Override
        public String getBotUsername() {
            return "testbot";
        }

        @Override
        public void sendHtmlMessage(long chatId, String text) {
            htmlMessages.add(text);
        }

        @Override
        public TelegramSentMessage sendMessageWithOpenAppButton(
                long chatId, String text, String buttonText, String chatType, String startParam) {
            buttonMessages.add(text);
            return new TelegramSentMessage(42L);
        }

        @Override
        public void pinMessage(long chatId, long messageId) {
            if (failNextPin) {
                failNextPin = false;
                throw new RuntimeException("pin failed");
            }
            pinnedMessageIds.add(messageId);
        }

        @Override
        public void sendMessage(long chatId, String text) {}

        @Override
        public com.fasterxml.jackson.databind.JsonNode getUpdates(long offset, int timeoutSeconds) {
            return null;
        }

        @Override
        public Optional<String> getChatTitle(long chatId) {
            return Optional.empty();
        }

        @Override
        public void deleteWebhook() {}

        @Override
        public void setMyCommands() {}
    }
}
