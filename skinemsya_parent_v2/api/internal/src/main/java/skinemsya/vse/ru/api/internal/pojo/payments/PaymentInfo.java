package skinemsya.vse.ru.api.internal.pojo.payments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Краткие данные платежа по долгу. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {

    private String status;
    private Long screenshotFileId;
}
