package skinemsya.vse.ru.subscriptions.api.dto;

import skinemsya.vse.ru.subscriptions.domain.SubscriptionCheckout;

public record SubscriptionCheckoutResponse(
        String method, String confirmationUrl, String confirmationToken, String paymentId, String invoiceLink) {

    public static SubscriptionCheckoutResponse from(SubscriptionCheckout checkout) {
        return new SubscriptionCheckoutResponse(
                checkout.method(),
                checkout.confirmationUrl(),
                checkout.confirmationToken(),
                checkout.paymentId(),
                checkout.invoiceLink());
    }
}
