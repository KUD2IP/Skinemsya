package skinemsya.vse.ru.integrations.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.receipt-vision")
public record ReceiptVisionProperties(
        String apiKey, String baseUrl, String model, Duration timeout, Integer maxImageSide) {

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

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
