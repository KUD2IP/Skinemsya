package skinemsya.vse.ru.debts.application;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(PaymentReadPort.class)
public class NoOpPaymentReadPort implements PaymentReadPort {

    @Override
    public Map<Long, PaymentInfo> findPaymentInfoByDebtIds(Collection<Long> debtIds) {
        return Collections.emptyMap();
    }
}
