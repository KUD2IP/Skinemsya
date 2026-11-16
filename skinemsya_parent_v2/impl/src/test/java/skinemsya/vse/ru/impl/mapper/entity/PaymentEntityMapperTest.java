package skinemsya.vse.ru.impl.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createPayerReminderJobEntity;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createPaymentEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга платежей и напоминаний. */
class PaymentEntityMapperTest {

    private final PaymentEntityMapper mapper = Mappers.getMapper(PaymentEntityMapper.class);

    /** Проверяет преобразование платежа в обе стороны без потери полей. */
    @Test
    void shouldMapPaymentInBothDirections() {
        var entity = createPaymentEntity();

        var payment = mapper.toPayment(entity);
        var mappedEntity = mapper.toEntity(payment);

        assertThat(payment.getDebtId()).isEqualTo(40L);
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }

    /** Проверяет преобразование напоминания в обе стороны без потери полей. */
    @Test
    void shouldMapReminderJobInBothDirections() {
        var entity = createPayerReminderJobEntity();

        var job = mapper.toJob(entity);
        var mappedEntity = mapper.toEntity(job);

        assertThat(job.getPaymentId()).isEqualTo(60L);
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }
}
