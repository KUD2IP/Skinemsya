package skinemsya.vse.ru.api.internal.pojo.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Данные пользователя, полученные из Telegram. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramUserData {

    private long telegramUserId;
    private String displayName;
    private String telegramUsername;
    private String photoUrl;
}
