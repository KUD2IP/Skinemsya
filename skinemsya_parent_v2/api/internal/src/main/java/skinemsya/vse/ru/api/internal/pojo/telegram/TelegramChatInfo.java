package skinemsya.vse.ru.api.internal.pojo.telegram;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Сведения о Telegram-чате. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramChatInfo {

    private long id;
    private String title;
    private String type;
}
