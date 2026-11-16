package skinemsya.vse.ru.api.internal.pojo.telegram;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Проверенные данные запуска Telegram Mini App. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramInitData {

    private TelegramIdentity identity;
    private Optional<TelegramChatContext> chat;
    private Optional<String> eventInviteCode;
    private Optional<String> groupInviteCode;
}
