package skinemsya.vse.ru.payments.domain;

public record PaymentDetailsView(
        long debtId,
        long eventId,
        long amountKopecks,
        long creditorId,
        String creditorName,
        String paymentDetails,
        String phone,
        String preferredBank,
        PaymentStatus status) {}
