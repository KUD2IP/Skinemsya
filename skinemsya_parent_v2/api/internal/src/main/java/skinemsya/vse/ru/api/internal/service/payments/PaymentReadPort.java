package skinemsya.vse.ru.api.internal.service.payments;

import java.util.Collection;
import java.util.Map;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentInfo;

/** Чтение платежей, нужное списку долгов. */
public interface PaymentReadPort {

    /** Возвращает сведения о платежах по идентификаторам долгов. */
    Map<Long, PaymentInfo> findPaymentInfoByDebtIds(Collection<Long> debtIds);
}
