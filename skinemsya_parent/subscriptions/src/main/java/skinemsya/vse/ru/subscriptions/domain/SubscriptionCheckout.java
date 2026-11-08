package skinemsya.vse.ru.subscriptions.domain;

public record SubscriptionCheckout(
        String method, String confirmationUrl, String confirmationToken, String paymentId, String invoiceLink) {}
