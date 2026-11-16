package skinemsya.vse.ru.api.internal.pojo.telegram;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Отправленное Telegram-сообщение. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramSentMessage {

    private long messageId;
}
