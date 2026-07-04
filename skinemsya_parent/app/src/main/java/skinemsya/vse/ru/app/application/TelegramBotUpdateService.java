package skinemsya.vse.ru.app.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.application.TelegramGroupWelcomeService;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.TelegramUserData;

@Service
public class TelegramBotUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotUpdateService.class);
    private static final String START_HINT = """
            Откройте приложение — вы уже в группе этого чата.

            Дальше: создайте мероприятие → укажите плательщика → добавьте позиции или чек.""";
    private static final String OPEN_BUTTON = "Открыть Skinemsya";
    private static final java.util.Set<String> JOINED_STATUSES = java.util.Set.of("member", "administrator", "creator");
    private static final java.util.Set<String> LEFT_STATUSES = java.util.Set.of("left", "kicked");

    private final TelegramGroupWelcomeService groupWelcomeService;
    private final TelegramBotClient telegramBotClient;
    private final UserService userService;
    private final GroupService groupService;

    public TelegramBotUpdateService(
            TelegramGroupWelcomeService groupWelcomeService,
            TelegramBotClient telegramBotClient,
            UserService userService,
            GroupService groupService
    ) {
        this.groupWelcomeService = groupWelcomeService;
        this.telegramBotClient = telegramBotClient;
        this.userService = userService;
        this.groupService = groupService;
    }

    public void handleUpdate(JsonNode update) {
        if (update == null || update.isNull()) {
            return;
        }
        if (update.hasNonNull("my_chat_member")) {
            groupWelcomeService.handleMyChatMemberUpdate(update);
            bootstrapGroupFromMyChatMember(update.path("my_chat_member"));
        }
        if (update.hasNonNull("message")) {
            handleMessage(update.path("message"));
        }
    }

    @Transactional
    void bootstrapGroupFromMyChatMember(JsonNode myChatMember) {
        if (!isGroupChat(myChatMember.path("chat"))) {
            return;
        }
        if (!isBotJoinedOrPromoted(myChatMember)) {
            return;
        }
        var addedBy = myChatMember.path("from");
        var chat = myChatMember.path("chat");
        ensureChatLinkedGroup(chat.path("id").asLong(), chatTitle(chat), addedBy);
    }

    @Transactional
    void handleMessage(JsonNode message) {
        var chat = message.path("chat");
        if (!isGroupChat(chat)) {
            return;
        }
        if (!isStartCommand(message.path("text").asText(""))) {
            return;
        }

        long chatId = chat.path("id").asLong();
        String chatType = chat.path("type").asText("");
        ensureChatLinkedGroup(chatId, chatTitle(chat), message.path("from"));

        try {
            telegramBotClient.sendMessageWithOpenAppButton(chatId, START_HINT, OPEN_BUTTON, chatType);
        } catch (RuntimeException ex) {
            log.error("Failed to send /start hint in chat {}", chatId, ex);
        }
    }

    private void ensureChatLinkedGroup(long chatId, String chatTitle, JsonNode telegramUser) {
        if (chatId == 0) {
            log.warn("Skipping chat-linked group bootstrap: chat id is missing");
            return;
        }
        if (telegramUser.isMissingNode() || telegramUser.path("id").isMissingNode()) {
            log.warn("Skipping chat-linked group bootstrap for chat {}: sender is missing", chatId);
            return;
        }

        long telegramUserId = telegramUser.path("id").asLong();
        if (telegramUserId <= 0 || telegramUser.path("is_bot").asBoolean(false)) {
            log.warn("Skipping chat-linked group bootstrap for chat {}: invalid sender", chatId);
            return;
        }
        String displayName = resolveDisplayName(telegramUser);
        String username = telegramUser.path("username").asText("");
        var user = userService.upsertFromTelegram(new TelegramUserData(telegramUserId, displayName, username));
        groupService.createFromChat(chatId, chatTitle, user.id());
    }

    private static boolean isBotJoinedOrPromoted(JsonNode myChatMember) {
        String oldStatus = myChatMember.path("old_chat_member").path("status").asText("");
        String newStatus = myChatMember.path("new_chat_member").path("status").asText("");
        return LEFT_STATUSES.contains(oldStatus) && JOINED_STATUSES.contains(newStatus)
                || "member".equals(oldStatus) && "administrator".equals(newStatus);
    }

    private static boolean isGroupChat(JsonNode chat) {
        String type = chat.path("type").asText("");
        return "group".equals(type) || "supergroup".equals(type);
    }

    private static boolean isStartCommand(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.equals("/start") || trimmed.startsWith("/start@") || trimmed.startsWith("/start ");
    }

    private static String chatTitle(JsonNode chat) {
        String title = chat.path("title").asText("");
        return title.isBlank() ? "Telegram chat" : title;
    }

    private static String resolveDisplayName(JsonNode telegramUser) {
        String firstName = telegramUser.path("first_name").asText("");
        if (!firstName.isBlank()) {
            return firstName;
        }
        String username = telegramUser.path("username").asText("");
        if (!username.isBlank()) {
            return username;
        }
        return "Telegram user";
    }
}
