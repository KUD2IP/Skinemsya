package skinemsya.vse.ru.integrations.infrastructure.telegram;

import java.util.Optional;
import skinemsya.vse.ru.common.invite.InviteCodes;

public final class TelegramStartParam {

    private static final String CHAT_PREFIX = "chat_";
    private static final String EVENT_PREFIX = "event_";
    private static final String GROUP_PREFIX = "group_";

    private TelegramStartParam() {}

    public static String forChat(long chatId) {
        return CHAT_PREFIX + chatId;
    }

    public static String forEventCode(String inviteCode) {
        return EVENT_PREFIX + inviteCode;
    }

    public static String forGroupCode(String inviteCode) {
        return GROUP_PREFIX + inviteCode;
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

    public static Optional<String> parseEventCode(String startParam) {
        return parseInviteCode(startParam, EVENT_PREFIX);
    }

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
