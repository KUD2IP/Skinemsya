package skinemsya.vse.ru.subscriptions.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.admin")
public record AdminProperties(String usernames) {

    public boolean matches(String username) {
        if (username == null || username.isBlank() || usernames == null || usernames.isBlank()) {
            return false;
        }
        String normalized = strip(username);
        for (String allowed : usernames.split(",")) {
            if (strip(allowed).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String strip(String value) {
        String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
    }
}
