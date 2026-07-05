package skinemsya.vse.ru.integrations.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TelegramGroupWelcomeService {

    private static final Logger log = LoggerFactory.getLogger(TelegramGroupWelcomeService.class);

    private static final String BUTTON_LABEL = "Открыть Skinemsya";
    private static final String PINNED_MESSAGE = "💸 Учёт и разделение расходов";
    private static final Set<String> JOINED_STATUSES = Set.of("member", "administrator", "creator");
    private static final Set<String> LEFT_STATUSES = Set.of("left", "kicked");

    private final TelegramBotClient botClient;
    private final ConcurrentHashMap<Long, Long> openAppMessageIdsByChat = new ConcurrentHashMap<>();
    private final Set<Long> pinnedOpenAppChats = ConcurrentHashMap.newKeySet();

    public TelegramGroupWelcomeService(TelegramBotClient botClient) {
        this.botClient = botClient;
    }

    public void handleMyChatMemberUpdate(JsonNode update) {
        JsonNode event = update.path("my_chat_member");
        if (event.isMissingNode()) {
            return;
        }

        JsonNode chat = event.path("chat");
        String chatType = chat.path("type").asText("");
        if (!"group".equals(chatType) && !"supergroup".equals(chatType)) {
            return;
        }

        String oldStatus = event.path("old_chat_member").path("status").asText("");
        String newStatus = event.path("new_chat_member").path("status").asText("");
        long chatId = chat.path("id").asLong();
        String chatTitle = chat.path("title").asText("группа");

        if (isBotJoined(oldStatus, newStatus)) {
            sendWelcome(chatId, chatTitle, chatType, formatUser(event.path("from")));
            return;
        }

        if (isBotPromotedToAdmin(oldStatus, newStatus)) {
            sendPinnedOpenAppButton(chatId, chatType);
        }
    }

    private void sendWelcome(long chatId, String chatTitle, String chatType, String addedBy) {
        try {
            botClient.sendHtmlMessage(
                    chatId,
                    """
                    👋 <b>Добро пожаловать в Skinemsya!</b>

                    Делите расходы в группе «<b>%s</b>» — просто и удобно.

                    📊 <b>Добавил(а) бота:</b>
                    • %s

                    ⚠️ <b>Важно:</b> админы чата добавляются сразу (если уже заходили в Skinemsya), остальные — когда откроют приложение или напишут в чат. Полный список участников Telegram боту недоступен."""
                            .formatted(chatTitle, addedBy));
        } catch (RuntimeException ex) {
            log.error("Failed to send welcome HTML message in chat {}", chatId, ex);
        }

        sendPinnedOpenAppButton(chatId, chatType);
    }

    private void sendPinnedOpenAppButton(long chatId, String chatType) {
        if (pinnedOpenAppChats.contains(chatId)) {
            return;
        }

        Long existingMessageId = openAppMessageIdsByChat.get(chatId);
        if (existingMessageId != null) {
            pinOpenAppButton(chatId, existingMessageId);
            return;
        }

        long messageId;
        try {
            var pinnedMessage = botClient.sendMessageWithOpenAppButton(chatId, PINNED_MESSAGE, BUTTON_LABEL, chatType);
            messageId = pinnedMessage.messageId();
            openAppMessageIdsByChat.put(chatId, messageId);
        } catch (RuntimeException ex) {
            log.warn(
                    "Could not send welcome button in chat {}: {}. "
                            + "Make the bot an administrator with pin permission and configure "
                            + "TELEGRAM_BOT_USERNAME or TELEGRAM_WEB_APP_SHORT_NAME.",
                    chatId,
                    ex.getMessage());
            return;
        }

        pinOpenAppButton(chatId, messageId);
    }

    private void pinOpenAppButton(long chatId, long messageId) {
        try {
            botClient.pinMessage(chatId, messageId);
            pinnedOpenAppChats.add(chatId);
            log.info("Pinned welcome message in chat: chatId={}", chatId);
        } catch (RuntimeException ex) {
            log.warn(
                    "Could not pin welcome button in chat {}: {}. "
                            + "Make the bot an administrator with pin permission.",
                    chatId,
                    ex.getMessage());
        }
    }

    private static boolean isBotJoined(String oldStatus, String newStatus) {
        return LEFT_STATUSES.contains(oldStatus) && JOINED_STATUSES.contains(newStatus);
    }

    private static boolean isBotPromotedToAdmin(String oldStatus, String newStatus) {
        return "member".equals(oldStatus) && "administrator".equals(newStatus);
    }

    private static String formatUser(JsonNode user) {
        if (user.isMissingNode()) {
            return "участник";
        }
        String username = user.path("username").asText("");
        if (!username.isBlank()) {
            return "@" + username;
        }
        String firstName = user.path("first_name").asText("");
        String lastName = user.path("last_name").asText("");
        String name = (firstName + " " + lastName).trim();
        return name.isBlank() ? "участник" : name;
    }
}
