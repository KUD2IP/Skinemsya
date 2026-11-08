package skinemsya.vse.ru.common.invite;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class InviteCodes {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES = 16;

    private InviteCodes() {}

    public static String random() {
        byte[] bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

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
