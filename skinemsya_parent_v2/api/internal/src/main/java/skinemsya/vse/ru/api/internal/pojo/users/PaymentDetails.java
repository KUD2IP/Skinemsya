package skinemsya.vse.ru.api.internal.pojo.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Платёжные реквизиты пользователя. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails {

    private String paymentDetails;
    private String phone;
    private String preferredBank;
}
