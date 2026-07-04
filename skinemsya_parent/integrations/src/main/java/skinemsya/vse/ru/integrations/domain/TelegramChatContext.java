package skinemsya.vse.ru.integrations.domain;

public record TelegramChatContext(
        long chatId,
        String title,
        String chatType
) {
}
