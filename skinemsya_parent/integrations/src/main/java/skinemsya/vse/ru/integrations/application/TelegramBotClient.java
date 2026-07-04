package skinemsya.vse.ru.integrations.application;

import com.fasterxml.jackson.databind.JsonNode;
import skinemsya.vse.ru.integrations.domain.TelegramSentMessage;

import java.util.Optional;

public interface TelegramBotClient {

    void initialize();

    String getBotUsername();

    void sendHtmlMessage(long chatId, String text);

    default TelegramSentMessage sendMessageWithOpenAppButton(
            long chatId,
            String text,
            String buttonText,
            String chatType
    ) {
        return sendMessageWithOpenAppButton(chatId, text, buttonText, chatType, null);
    }

    TelegramSentMessage sendMessageWithOpenAppButton(
            long chatId,
            String text,
            String buttonText,
            String chatType,
            String startParam
    );

    void pinMessage(long chatId, long messageId);

    void sendMessage(long chatId, String text);

    JsonNode getUpdates(long offset, int timeoutSeconds);

    /** Название группового чата из Telegram Bot API. */
    Optional<String> getChatTitle(long chatId);

    void deleteWebhook();

    void setMyCommands();
}
