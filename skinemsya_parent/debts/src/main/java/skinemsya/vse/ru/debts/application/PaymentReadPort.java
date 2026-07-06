package skinemsya.vse.ru.debts.application;

import java.util.Collection;
import java.util.Map;

public interface PaymentReadPort {

    Map<Long, PaymentInfo> findPaymentInfoByDebtIds(Collection<Long> debtIds);
}
