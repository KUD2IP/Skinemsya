package skinemsya.vse.ru.impl.support.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Настройки интеграции с Telegram Bot API и Mini App. */
@ConfigurationProperties(prefix = "skinemsya.telegram")
public record TelegramIntegrationProperties(
        String botToken,
        long authMaxAgeSeconds,
        String miniAppUrl,
        String webhookSecret,
        String botUsername,
        String webAppShortName,
        Boolean botPollingEnabled) {

    /** Применяет значения по умолчанию. */
    public TelegramIntegrationProperties {
        if (authMaxAgeSeconds <= 0) {
            authMaxAgeSeconds = 86_400;
        }
    }

    /** Возвращает {@code true}, если webhook защищён секретом. */
    public boolean isWebhookProtected() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    /** Возвращает {@code true}, если включён long polling бота. */
    public boolean isBotPollingEnabled() {
        return botPollingEnabled == null || botPollingEnabled;
    }
}
