package skinemsya.vse.ru.impl.service.payments;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentInfo;
import skinemsya.vse.ru.api.internal.repository.payments.PaymentRepository;
import skinemsya.vse.ru.api.internal.service.payments.PaymentReadPort;

/** Читает статус платежа и файл скриншота для списка долгов. */
@Component
@Primary
@RequiredArgsConstructor
public class PaymentReadPortImpl implements PaymentReadPort {

    private final PaymentRepository paymentRepository;

    @Override
    public Map<Long, PaymentInfo> findPaymentInfoByDebtIds(Collection<Long> debtIds) {
        if (debtIds == null || debtIds.isEmpty()) {
            return Map.of();
        }
        return paymentRepository.findByDebtIdIn(debtIds).stream()
                .collect(Collectors.toMap(
                        payment -> payment.getDebtId(),
                        payment -> new PaymentInfo(payment.getStatus().name(), payment.getScreenshotFileId())));
    }
}
