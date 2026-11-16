package skinemsya.vse.ru.api.internal.pojo.telegram;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Подтверждённая личность пользователя Telegram. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramIdentity {

    private long telegramUserId;
    private String displayName;
    private Instant authDate;
    private String telegramUsername;
    private String photoUrl;
}
