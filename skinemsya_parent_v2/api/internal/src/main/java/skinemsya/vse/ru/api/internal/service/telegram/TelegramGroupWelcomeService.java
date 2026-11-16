package skinemsya.vse.ru.api.internal.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;

/** Обработка событий добавления бота в Telegram-группу. */
public interface TelegramGroupWelcomeService {

    /** Обрабатывает обновление статуса участника бота в чате. */
    void handleMyChatMemberUpdate(JsonNode update);
}
