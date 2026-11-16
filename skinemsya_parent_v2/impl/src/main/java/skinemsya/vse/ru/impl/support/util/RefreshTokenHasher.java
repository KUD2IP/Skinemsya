package skinemsya.vse.ru.impl.support.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.experimental.UtilityClass;

/** Криптографическое хеширование исходного refresh-токена. */
@UtilityClass
public class RefreshTokenHasher {


    /** Вычисляет SHA-256 хекс-представление исходного токена. */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
