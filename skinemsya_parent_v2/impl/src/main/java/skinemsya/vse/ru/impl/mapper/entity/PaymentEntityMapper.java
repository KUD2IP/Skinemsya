package skinemsya.vse.ru.impl.mapper.entity;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.payments.PayerReminderJob;
import skinemsya.vse.ru.api.internal.pojo.payments.Payment;
import skinemsya.vse.ru.data.entity.payments.PayerReminderJobEntity;
import skinemsya.vse.ru.data.entity.payments.PaymentEntity;

/** MapStruct-маппер между сущностями платежей и внутренними POJO. */
@Mapper(componentModel = "spring")
public interface PaymentEntityMapper {

    /** Конвертирует сущность платежа во внутренний POJO. */
    Payment toPayment(PaymentEntity entity);

    /** Конвертирует внутренний POJO платежа в сущность. */
    PaymentEntity toEntity(Payment payment);

    /** Конвертирует сущность напоминания во внутренний POJO. */
    PayerReminderJob toJob(PayerReminderJobEntity entity);

    /** Конвертирует внутренний POJO напоминания в сущность. */
    PayerReminderJobEntity toEntity(PayerReminderJob job);
}
