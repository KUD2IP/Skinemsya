package skinemsya.vse.ru.common.event;

public record DebtorConfirmed(
        long eventId,
        long paymentId,
        long debtorId,
        long creditorId,
        long amountKopecks,
        String debtorName
) {
}
