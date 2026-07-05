package skinemsya.vse.ru.common.event;

public record PaymentDisputed(long eventId, long paymentId, long debtorId, long creditorId, long amountKopecks) {}
