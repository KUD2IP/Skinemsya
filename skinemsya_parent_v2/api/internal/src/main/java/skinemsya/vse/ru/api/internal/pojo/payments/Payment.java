package skinemsya.vse.ru.api.internal.pojo.payments;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;

/** Платёж по долгу. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private long id;
    private long debtId;
    private PaymentStatus status;
    private Long screenshotFileId;
    private Instant debtorConfirmedAt;
    private Instant payerConfirmedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
