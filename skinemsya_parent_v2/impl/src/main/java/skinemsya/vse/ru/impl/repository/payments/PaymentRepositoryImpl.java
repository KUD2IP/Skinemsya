package skinemsya.vse.ru.impl.repository.payments;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;
import skinemsya.vse.ru.api.internal.pojo.payments.Payment;
import skinemsya.vse.ru.api.internal.repository.payments.PaymentRepository;
import skinemsya.vse.ru.impl.mapper.entity.PaymentEntityMapper;

/** Адаптер между внутренним портом платежей и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentEntityMapper paymentEntityMapper;

    @Override
    public Optional<Payment> findById(long paymentId) {
        return paymentJpaRepository.findById(paymentId).map(paymentEntityMapper::toPayment);
    }

    @Override
    public Optional<Payment> findByDebtId(long debtId) {
        return paymentJpaRepository.findByDebtId(debtId).map(paymentEntityMapper::toPayment);
    }

    @Override
    public List<Payment> findByDebtIdIn(Collection<Long> debtIds) {
        return paymentJpaRepository.findByDebtIdIn(debtIds).stream()
                .map(paymentEntityMapper::toPayment)
                .toList();
    }

    @Override
    public Optional<Payment> findByScreenshotFileId(long screenshotFileId) {
        return paymentJpaRepository.findByScreenshotFileId(screenshotFileId).map(paymentEntityMapper::toPayment);
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentJpaRepository.findByStatus(status).stream()
                .map(paymentEntityMapper::toPayment)
                .toList();
    }

    @Override
    public Payment save(Payment payment) {
        var entity = paymentEntityMapper.toEntity(payment);
        if (payment.getId() == 0L) {
            entity.setId(null);
        }
        return paymentEntityMapper.toPayment(paymentJpaRepository.save(entity));
    }
}
