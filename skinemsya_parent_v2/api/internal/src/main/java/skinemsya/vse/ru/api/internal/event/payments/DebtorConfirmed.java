package skinemsya.vse.ru.api.internal.event.payments;

/** Должник отметил перевод. */
public record DebtorConfirmed(
        long eventId, long paymentId, long debtorId, long creditorId, long amountKopecks, String debtorName) {}
