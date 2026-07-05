package skinemsya.vse.ru.users.application;

import java.util.Locale;
import java.util.regex.Pattern;

final class TelegramUsernameNormalizer {

    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{5,32}$");

    private TelegramUsernameNormalizer() {}

    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        var value = raw.trim();
        if (value.startsWith("@")) {
            value = value.substring(1);
        }
        if (value.isBlank()) {
            return null;
        }
        value = value.toLowerCase(Locale.ROOT);
        if (!USERNAME.matcher(value).matches()) {
            return null;
        }
        return value;
    }
}
