package skinemsya.vse.ru.integrations.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.telegram")
public record TelegramIntegrationProperties(
        String botToken,
        long authMaxAgeSeconds,
        String miniAppUrl,
        String webhookSecret,
        String botUsername,
        String webAppShortName,
        Boolean botPollingEnabled
) {
    public TelegramIntegrationProperties {
        if (authMaxAgeSeconds <= 0) {
            authMaxAgeSeconds = 86_400;
        }
    }

    public boolean isWebhookProtected() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public boolean isBotPollingEnabled() {
        return botPollingEnabled == null || botPollingEnabled;
    }
}
