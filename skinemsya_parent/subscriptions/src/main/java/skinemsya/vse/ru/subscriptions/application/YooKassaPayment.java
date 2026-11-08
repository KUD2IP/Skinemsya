package skinemsya.vse.ru.subscriptions.application;

public record YooKassaPayment(
        String id,
        String status,
        String confirmationUrl,
        String confirmationToken,
        String paymentMethodId,
        Long userId,
        String kind) {}
