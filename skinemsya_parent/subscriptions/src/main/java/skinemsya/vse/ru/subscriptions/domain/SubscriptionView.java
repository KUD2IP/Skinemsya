package skinemsya.vse.ru.subscriptions.domain;

import java.time.Instant;

public record SubscriptionView(
        boolean active,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        SubscriptionProvider provider,
        long priceKopecks,
        int stars,
        boolean yookassaAvailable,
        boolean starsAvailable) {}
