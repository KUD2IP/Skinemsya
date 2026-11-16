package skinemsya.vse.ru.api.internal.event.payments;

/** Плательщик оспорил перевод. */
public record PaymentDisputed(long eventId, long paymentId, long debtorId, long creditorId, long amountKopecks) {}
