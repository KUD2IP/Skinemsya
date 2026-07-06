package skinemsya.vse.ru.payments.application;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.debts.application.PaymentInfo;
import skinemsya.vse.ru.debts.application.PaymentReadPort;
import skinemsya.vse.ru.payments.infrastructure.persistence.PaymentRepository;

@Component
@Primary
public class PaymentReadPortImpl implements PaymentReadPort {

    private final PaymentRepository paymentRepository;

    public PaymentReadPortImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Map<Long, PaymentInfo> findPaymentInfoByDebtIds(Collection<Long> debtIds) {
        if (debtIds.isEmpty()) {
            return Map.of();
        }
        return paymentRepository.findByDebtIdIn(debtIds.stream().toList()).stream()
                .collect(Collectors.toMap(
                        payment -> payment.getDebtId(),
                        payment -> new PaymentInfo(payment.getStatus().name(), payment.getScreenshotFileId())));
    }
}
