package skinemsya.vse.ru.impl.support.util;

import java.util.Optional;
import lombok.experimental.UtilityClass;

/** Кодирование параметров запуска Telegram Mini App. */
@UtilityClass
public class TelegramStartParam {

    private static final String CHAT_PREFIX = "chat_";
    private static final String EVENT_PREFIX = "event_";
    private static final String GROUP_PREFIX = "group_";


    /** Формирует start parameter для привязки Telegram-чата. */
    public static String forChat(long chatId) {
        return CHAT_PREFIX + chatId;
    }

    /** Формирует start parameter для приглашения в событие. */
    public static String forEventCode(String inviteCode) {
        return EVENT_PREFIX + inviteCode;
    }

    /** Формирует start parameter для приглашения в группу. */
    public static String forGroupCode(String inviteCode) {
        return GROUP_PREFIX + inviteCode;
    }

    /** Извлекает идентификатор чата из start parameter. */
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

    /** Извлекает код приглашения события из start parameter. */
    public static Optional<String> parseEventCode(String startParam) {
        return parseInviteCode(startParam, EVENT_PREFIX);
    }

    /** Извлекает код приглашения группы из start parameter. */
    public static Optional<String> parseGroupCode(String startParam) {
        return parseInviteCode(startParam, GROUP_PREFIX);
    }

    private static Optional<String> parseInviteCode(String startParam, String prefix) {
        if (startParam == null || !startParam.startsWith(prefix)) {
            return Optional.empty();
        }
        String raw = startParam.substring(prefix.length());
        return InviteCodes.isToken(raw) ? Optional.of(raw) : Optional.empty();
    }
}
