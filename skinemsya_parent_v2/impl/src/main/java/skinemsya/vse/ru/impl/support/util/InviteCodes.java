package skinemsya.vse.ru.impl.support.util;

import java.security.SecureRandom;
import java.util.HexFormat;
import lombok.experimental.UtilityClass;

/** Генерация и проверка неугадываемых кодов приглашений. */
@UtilityClass
public class InviteCodes {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES = 16;


    /** Генерирует новый 32-символьный hex-код приглашения. */
    public static String random() {
        byte[] bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Проверяет формат неугадываемого кода приглашения. */
    public static boolean isToken(String value) {
        if (value == null || value.length() != BYTES * 2) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch < '0' || ch > '9') && (ch < 'a' || ch > 'f')) {
                return false;
            }
        }
        return true;
    }
}
