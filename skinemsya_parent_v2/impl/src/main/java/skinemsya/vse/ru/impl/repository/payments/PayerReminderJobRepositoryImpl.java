package skinemsya.vse.ru.impl.repository.payments;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.pojo.payments.PayerReminderJob;
import skinemsya.vse.ru.api.internal.repository.payments.PayerReminderJobRepository;
import skinemsya.vse.ru.impl.mapper.entity.PaymentEntityMapper;

/** Адаптер между внутренним портом напоминаний и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class PayerReminderJobRepositoryImpl implements PayerReminderJobRepository {

    private final PayerReminderJobJpaRepository payerReminderJobJpaRepository;
    private final PaymentEntityMapper paymentEntityMapper;

    @Override
    public List<PayerReminderJob> findDue(Instant before) {
        return payerReminderJobJpaRepository.findBySentAtIsNullAndScheduledAtBefore(before).stream()
                .map(paymentEntityMapper::toJob)
                .toList();
    }

    @Override
    public Optional<PayerReminderJob> findByPaymentId(long paymentId) {
        return payerReminderJobJpaRepository.findByPaymentId(paymentId).map(paymentEntityMapper::toJob);
    }

    @Override
    public PayerReminderJob save(PayerReminderJob job) {
        var entity = paymentEntityMapper.toEntity(job);
        if (job.getId() == 0L) {
            entity.setId(null);
        }
        return paymentEntityMapper.toJob(payerReminderJobJpaRepository.save(entity));
    }
}
