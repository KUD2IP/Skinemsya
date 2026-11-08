package skinemsya.vse.ru.subscriptions.api.dto;

import java.time.Instant;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionProvider;
import skinemsya.vse.ru.subscriptions.domain.SubscriptionView;

public record SubscriptionResponse(
        boolean active,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        SubscriptionProvider provider,
        long priceKopecks,
        int stars,
        boolean yookassaAvailable,
        boolean starsAvailable) {

    public static SubscriptionResponse from(SubscriptionView view) {
        return new SubscriptionResponse(
                view.active(),
                view.currentPeriodEnd(),
                view.cancelAtPeriodEnd(),
                view.provider(),
                view.priceKopecks(),
                view.stars(),
                view.yookassaAvailable(),
                view.starsAvailable());
    }
}
