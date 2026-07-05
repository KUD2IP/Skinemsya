package skinemsya.vse.ru.integrations.infrastructure.telegram;

import java.util.Optional;

public final class TelegramStartParam {

    private static final String CHAT_PREFIX = "chat_";
    private static final String EVENT_PREFIX = "event_";

    private TelegramStartParam() {}

    public static String forChat(long chatId) {
        return CHAT_PREFIX + chatId;
    }

    public static String forEvent(long eventId) {
        return EVENT_PREFIX + eventId;
    }

    public static Optional<Long> parseChatId(String startParam) {
        if (startParam == null || !startParam.startsWith(CHAT_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(startParam.substring(CHAT_PREFIX.length())));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Long> parseEventId(String startParam) {
        if (startParam == null || !startParam.startsWith(EVENT_PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(startParam.substring(EVENT_PREFIX.length())));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
