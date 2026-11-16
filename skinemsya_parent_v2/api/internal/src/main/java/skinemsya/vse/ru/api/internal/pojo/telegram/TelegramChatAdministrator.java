package skinemsya.vse.ru.api.internal.pojo.telegram;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Администратор Telegram-чата. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramChatAdministrator {

    private long telegramUserId;
    private boolean bot;
}
