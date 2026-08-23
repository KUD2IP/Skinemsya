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

    private static final String PRIVATE_START =
            """
            👋 Привет! Это <b>Skinemsya</b>.

            💸 Делите общие расходы в компании: один платит по чеку, остальные скидываются. Сборы живут в группах Telegram.

            🚀 <b>Как начать</b>
            1. Добавьте бота в групповой чат.
            2. Откройте приложение из этого чата — вы сразу попадёте в группу Skinemsya.
            3. Создайте сбор, укажите, кто платил, добавьте позиции или фото чека.
            4. Каждый выбирает своё и отмечает перевод в приложении.
            5. Плательщик подтверждает переводы и закрывает сбор.

            💬 Напоминания и статусы приходят в групповой чат, не в личку.

            ℹ️ /help — подробнее, /open — кнопка приложения.""";

    private static final String GROUP_START =
            """
            ✅ Вы в группе этого чата. Создайте сбор, добавьте позиции или чек.""";

    private static final String PRIVATE_HELP =
            """
            💸 <b>Skinemsya</b> — приложение, чтобы скинуться по чеку в компании.

            📋 <b>Как пользоваться</b>
            • Добавьте бота в групповой чат друзей или поездки.
            • Откройте приложение из этого чата — вы автоматически попадёте в группу Skinemsya.
            • В приложении будут только те, кто уже заходил: полный список участников Telegram боту недоступен.
            • Создайте сбор, выберите плательщика и добавьте позиции вручную или с фото чека.
            • Каждый выбирает своё и отмечает перевод. Плательщик подтверждает переводы и закрывает сбор.

            🔗 Можно создать отдельную группу без привязки к чату — из приложения, и пригласить друзей ссылкой.

            ⌨️ <b>Команды</b>
            /start — приветствие и как начать
            /help — эта справка
            /open — сообщение с кнопкой «Открыть Skinemsya»

            💬 Напоминания и статусы приходят в групповой чат.""";

    private static final String GROUP_HELP =
            """
            💸 <b>Skinemsya</b> — приложение, чтобы скинуться по чеку в этом чате.

            📋 <b>Как пользоваться</b>
            • Откройте приложение из этого чата — вы автоматически попадёте в группу Skinemsya.
            • В приложении будут только те, кто уже заходил: полный список участников Telegram боту недоступен.
            • Создайте сбор, выберите плательщика и добавьте позиции вручную или с фото чека.
            • Каждый выбирает своё и отмечает перевод. Плательщик подтверждает переводы и закрывает сбор.

            🔗 Можно создать отдельную группу без привязки к чату — из приложения, и пригласить друзей ссылкой.

            ⌨️ <b>Команды</b>
            /start — открыть приложение из чата
            /help — эта справка
            /open — сообщение с кнопкой «Открыть Skinemsya»

            💬 Напоминания и статусы приходят в этот чат.""";

    private static final String OPEN_HINT = "📲 Откройте Skinemsya, чтобы делить расходы.";
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
            GroupService groupService) {
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
        BotCommand command = parseCommand(message.path("text").asText(""));
        if (command == null) {
            return;
        }

        var chat = message.path("chat");
        String chatType = chat.path("type").asText("");
        long chatId = chat.path("id").asLong();

        if ("private".equals(chatType)) {
            sendOpenAppReply(chatId, "private", replyText(command, true));
            return;
        }

        if (!isGroupChat(chat)) {
            return;
        }

        if (command == BotCommand.START) {
            ensureChatLinkedGroup(chatId, chatTitle(chat), message.path("from"));
        }

        sendOpenAppReply(chatId, chatType, replyText(command, false));
    }

    private void sendOpenAppReply(long chatId, String chatType, String text) {
        try {
            telegramBotClient.sendMessageWithOpenAppButton(chatId, text, OPEN_BUTTON, chatType);
        } catch (RuntimeException ex) {
            log.error("Failed to send bot command reply in chat {}", chatId, ex);
        }
    }

    private static String replyText(BotCommand command, boolean privateChat) {
        return switch (command) {
            case START -> privateChat ? PRIVATE_START : GROUP_START;
            case HELP -> privateChat ? PRIVATE_HELP : GROUP_HELP;
            case OPEN -> OPEN_HINT;
        };
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

    private static BotCommand parseCommand(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String token = text.trim().split("\\s+", 2)[0];
        int at = token.indexOf('@');
        if (at >= 0) {
            token = token.substring(0, at);
        }
        return switch (token) {
            case "/start" -> BotCommand.START;
            case "/help" -> BotCommand.HELP;
            case "/open" -> BotCommand.OPEN;
            default -> null;
        };
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

    private enum BotCommand {
        START,
        HELP,
        OPEN
    }
}
