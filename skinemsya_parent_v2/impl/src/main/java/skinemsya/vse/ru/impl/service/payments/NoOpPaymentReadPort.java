package skinemsya.vse.ru.impl.service.payments;

import java.util.Collection;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentInfo;
import skinemsya.vse.ru.api.internal.service.payments.PaymentReadPort;

/** Пустой ответ о платежах до переноса модуля платежей. */
@Component
@ConditionalOnMissingBean(PaymentReadPort.class)
public class NoOpPaymentReadPort implements PaymentReadPort {

    @Override
    public Map<Long, PaymentInfo> findPaymentInfoByDebtIds(Collection<Long> debtIds) {
        return Map.of();
    }
}
