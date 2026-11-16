package skinemsya.vse.ru.api.internal.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramSentMessage;

/** Клиент Telegram Bot API. */
public interface TelegramBotClient {

    /** Инициализирует клиента и разрешает сведения о боте. */
    void initialize();

    /** Возвращает username бота. */
    String getBotUsername();

    /** Отправляет HTML-сообщение в чат. */
    void sendHtmlMessage(long chatId, String text);

    /** Отправляет сообщение с кнопкой открытия Mini App. */
    default TelegramSentMessage sendMessageWithOpenAppButton(
            long chatId, String text, String buttonText, String chatType) {
        return sendMessageWithOpenAppButton(chatId, text, buttonText, chatType, null);
    }

    /** Отправляет сообщение с кнопкой открытия Mini App и параметром запуска. */
    TelegramSentMessage sendMessageWithOpenAppButton(
            long chatId, String text, String buttonText, String chatType, String startParam);

    /** Закрепляет сообщение в чате. */
    void pinMessage(long chatId, long messageId);

    /** Отправляет обычное текстовое сообщение. */
    void sendMessage(long chatId, String text);

    /** Получает обновления через long polling. */
    JsonNode getUpdates(long offset, int timeoutSeconds);

    /** Возвращает название группового чата. */
    Optional<String> getChatTitle(long chatId);

    /** Удаляет webhook. */
    void deleteWebhook();

    /** Устанавливает команды бота. */
    void setMyCommands();

    /** Устанавливает описание бота. */
    void setMyDescription();

    /** Устанавливает короткое описание бота. */
    void setMyShortDescription();

    /** Формирует deep link для открытия Mini App. */
    String buildMiniAppDeepLink(String startParam);

    /** Создаёт ссылку на Stars-подписку. */
    default String createStarsSubscriptionLink(String title, String description, String payload, int stars) {
        throw new UnsupportedOperationException("Stars invoices are not available");
    }

    /** Отвечает на pre-checkout запрос Stars. */
    default void answerPreCheckoutQuery(String queryId, boolean ok, String errorMessage) {
        throw new UnsupportedOperationException("Stars invoices are not available");
    }

    /** Отменяет Stars-подписку пользователя. */
    default void cancelStarSubscription(long telegramUserId, String chargeId) {
        throw new UnsupportedOperationException("Stars invoices are not available");
    }
}
