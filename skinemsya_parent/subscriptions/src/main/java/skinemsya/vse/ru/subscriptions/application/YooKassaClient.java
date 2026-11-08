package skinemsya.vse.ru.subscriptions.application;

public interface YooKassaClient {

    YooKassaPayment createCheckout(long userId, String phone, String idempotenceKey, boolean sbp);

    YooKassaPayment createRenewal(long userId, String paymentMethodId, String phone, String idempotenceKey);

    YooKassaPayment getPayment(String paymentId);
}
