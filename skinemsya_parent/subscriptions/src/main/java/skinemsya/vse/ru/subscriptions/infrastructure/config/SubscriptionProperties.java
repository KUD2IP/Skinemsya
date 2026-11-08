package skinemsya.vse.ru.subscriptions.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.subscription")
public record SubscriptionProperties(
        boolean enforced, long priceKopecks, int periodDays, int stars, int maxParsesPerDay, YooKassa yookassa) {

    public SubscriptionProperties {
        if (priceKopecks <= 0) {
            priceKopecks = 4900;
        }
        if (periodDays <= 0) {
            periodDays = 30;
        }
        if (stars <= 0) {
            stars = 30;
        }
        if (maxParsesPerDay <= 0) {
            maxParsesPerDay = 100;
        }
        if (yookassa == null) {
            yookassa = new YooKassa("", "", "", 1, true);
        }
    }

    public record YooKassa(String shopId, String secretKey, String returnUrl, int vatCode, boolean receiptEnabled) {

        public YooKassa {
            if (vatCode <= 0) {
                vatCode = 1;
            }
        }

        public boolean isConfigured() {
            return shopId != null && !shopId.isBlank() && secretKey != null && !secretKey.isBlank();
        }
    }
}
