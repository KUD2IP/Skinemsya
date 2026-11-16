package skinemsya.vse.ru.api.internal.service.telegram;

import java.util.Optional;

/** Сведения о Telegram-чатах. */
public interface TelegramChatService {

    /** Возвращает название группового чата. */
    Optional<String> getChatTitle(long chatId);
}
