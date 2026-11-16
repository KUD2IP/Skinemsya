package skinemsya.vse.ru.api.internal.pojo.users;

/** Проверки реквизитов, доступных для получения перевода. */
public final class PayoutRequisites {

    private PayoutRequisites() {}

    /** Проверяет наличие хотя бы одного реквизита для перевода. */
    public static boolean hasAny(PaymentDetails details) {
        return isNotBlank(details.getPaymentDetails()) || isNotBlank(details.getPhone());
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
