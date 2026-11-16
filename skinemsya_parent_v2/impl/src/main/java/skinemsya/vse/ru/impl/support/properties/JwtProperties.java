package skinemsya.vse.ru.impl.support.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Настройки JWT и времени жизни токенов. */
@ConfigurationProperties(prefix = "skinemsya.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {

    /** Устанавливает значения времени жизни по умолчанию. */
    public JwtProperties {
        if (accessTtl == null) {
            accessTtl = Duration.ofMinutes(15);
        }
        if (refreshTtl == null) {
            refreshTtl = Duration.ofDays(7);
        }
    }
}
