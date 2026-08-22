package skinemsya.vse.ru.integrations.infrastructure.telegram;

import java.util.Optional;

public final class TelegramStartParam {

    private static final String CHAT_PREFIX = "chat_";
    private static final String EVENT_PREFIX = "event_";
    private static final String GROUP_PREFIX = "group_";

    private TelegramStartParam() {}

    public static String forChat(long chatId) {
        return CHAT_PREFIX + chatId;
    }

    public static String forEvent(long eventId) {
        return EVENT_PREFIX + eventId;
    }

    public static String forGroup(long groupId) {
        return GROUP_PREFIX + groupId;
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
        return parsePrefixedId(startParam, EVENT_PREFIX);
    }

    public static Optional<Long> parseGroupId(String startParam) {
        return parsePrefixedId(startParam, GROUP_PREFIX);
    }

    private static Optional<Long> parsePrefixedId(String startParam, String prefix) {
        if (startParam == null || !startParam.startsWith(prefix)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(startParam.substring(prefix.length())));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
