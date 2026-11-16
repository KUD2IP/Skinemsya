package skinemsya.vse.ru.api.internal.pojo.payments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;

/** Реквизиты плательщика и статус платежа для должника. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailsView {

    private long debtId;
    private long eventId;
    private long amountKopecks;
    private long creditorId;
    private String creditorName;
    private String paymentDetails;
    private String phone;
    private String preferredBank;
    private PaymentStatus status;
}
