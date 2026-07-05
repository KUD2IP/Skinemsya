package skinemsya.vse.ru.integrations.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.ml-service")
public record MlServiceProperties(String url, Duration timeout) {
    public MlServiceProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
    }

    public boolean isConfigured() {
        return url != null && !url.isBlank();
    }
}
