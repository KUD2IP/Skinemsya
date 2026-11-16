package skinemsya.vse.ru.api.internal.pojo.telegram;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Контекст Telegram-чата, из которого открыто приложение. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramChatContext {

    private long chatId;
    private String title;
    private String chatType;
}
