package skinemsya.vse.ru.impl.service.telegram;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramBotClient;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramChatService;

/** Адаптер сведений о Telegram-чатах поверх клиента Bot API. */
@Service
@RequiredArgsConstructor
public class TelegramChatServiceImpl implements TelegramChatService {

    private final TelegramBotClient telegramBotClient;

    @Override
    public Optional<String> getChatTitle(long chatId) {
        return telegramBotClient.getChatTitle(chatId);
    }
}
