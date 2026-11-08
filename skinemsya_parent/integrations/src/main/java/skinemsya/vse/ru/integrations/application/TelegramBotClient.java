package skinemsya.vse.ru.integrations.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import skinemsya.vse.ru.integrations.domain.TelegramSentMessage;

public interface TelegramBotClient {

    void initialize();

    String getBotUsername();

    void sendHtmlMessage(long chatId, String text);

    default TelegramSentMessage sendMessageWithOpenAppButton(
            long chatId, String text, String buttonText, String chatType) {
        return sendMessageWithOpenAppButton(chatId, text, buttonText, chatType, null);
    }

    TelegramSentMessage sendMessageWithOpenAppButton(
            long chatId, String text, String buttonText, String chatType, String startParam);

    void pinMessage(long chatId, long messageId);

    void sendMessage(long chatId, String text);

    JsonNode getUpdates(long offset, int timeoutSeconds);

    /** Название группового чата из Telegram Bot API. */
    Optional<String> getChatTitle(long chatId);

    void deleteWebhook();

    void setMyCommands();

    void setMyDescription();

    void setMyShortDescription();

    String buildMiniAppDeepLink(String startParam);

    default String createStarsSubscriptionLink(String title, String description, String payload, int stars) {
        throw new UnsupportedOperationException("Stars invoices are not available");
    }

    default void answerPreCheckoutQuery(String queryId, boolean ok, String errorMessage) {
        throw new UnsupportedOperationException("Stars invoices are not available");
    }

    default void cancelStarSubscription(long telegramUserId, String chargeId) {
        throw new UnsupportedOperationException("Stars invoices are not available");
    }
}
