package skinemsya.vse.ru.users.domain;

/** Проверка, что у плательщика есть реквизиты для приёма перевода (СБП или карта). */
public final class PayoutRequisites {

    private PayoutRequisites() {}

    public static boolean hasAny(PaymentDetails details) {
        return isNotBlank(details.paymentDetails()) || isNotBlank(details.phone());
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
