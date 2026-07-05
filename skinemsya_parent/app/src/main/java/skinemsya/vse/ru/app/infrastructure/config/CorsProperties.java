package skinemsya.vse.ru.app.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {
    public CorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            allowedOriginPatterns = List.of("http://localhost:*", "http://127.0.0.1:*", "https://web.telegram.org");
        }
    }

    public static List<String> parsePatterns(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
