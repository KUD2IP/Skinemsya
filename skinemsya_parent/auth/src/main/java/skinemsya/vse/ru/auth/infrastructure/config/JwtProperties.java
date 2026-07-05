package skinemsya.vse.ru.auth.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {
    public JwtProperties {
        if (accessTtl == null) {
            accessTtl = Duration.ofMinutes(15);
        }
        if (refreshTtl == null) {
            refreshTtl = Duration.ofDays(7);
        }
    }
}
