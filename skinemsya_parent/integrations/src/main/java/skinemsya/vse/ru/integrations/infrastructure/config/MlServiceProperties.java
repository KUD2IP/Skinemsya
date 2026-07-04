package skinemsya.vse.ru.integrations.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "skinemsya.ml-service")
public record MlServiceProperties(
        String url,
        Duration timeout
) {
    public MlServiceProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
    }

    public boolean isConfigured() {
        return url != null && !url.isBlank();
    }
}
