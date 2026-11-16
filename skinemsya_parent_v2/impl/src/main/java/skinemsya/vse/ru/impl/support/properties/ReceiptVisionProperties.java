package skinemsya.vse.ru.impl.support.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Настройки распознавания чеков через OpenRouter vision-модель. */
@ConfigurationProperties(prefix = "skinemsya.receipt-vision")
public record ReceiptVisionProperties(
        String apiKey, String baseUrl, String model, Duration timeout, Integer maxImageSide) {

    /** Применяет значения по умолчанию. */
    public ReceiptVisionProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }
        if (model == null || model.isBlank()) {
            model = "qwen/qwen3.8-flash";
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = Duration.ofSeconds(30);
        }
        if (maxImageSide == null || maxImageSide < 640) {
            maxImageSide = 1600;
        }
    }

    /** Возвращает {@code true}, если задан ключ API. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
