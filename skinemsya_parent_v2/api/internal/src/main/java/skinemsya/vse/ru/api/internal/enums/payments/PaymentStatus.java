package skinemsya.vse.ru.api.internal.enums.payments;

/** Статус платежа по долгу. */
public enum PaymentStatus {
    CREATED,
    DEBTOR_CONFIRMED,
    PAYER_CONFIRMED,
    CANCELLED,
    DISPUTED
}
