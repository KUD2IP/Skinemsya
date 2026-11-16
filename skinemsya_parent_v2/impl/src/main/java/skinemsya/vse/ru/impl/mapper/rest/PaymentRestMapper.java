package skinemsya.vse.ru.impl.mapper.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.enums.payments.PaymentStatus;
import skinemsya.vse.ru.api.internal.pojo.payments.Payment;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentDetailsView;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentDetailsResponse;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentResponse;

/** MapStruct-маппер между внутренними POJO платежей и REST DTO. */
@Mapper(componentModel = "spring")
public interface PaymentRestMapper {

    /** Конвертирует платёж в REST-ответ. */
    PaymentResponse toResponse(Payment payment);

    /** Конвертирует реквизиты для оплаты в REST-ответ. */
    PaymentDetailsResponse toResponse(PaymentDetailsView view);

    /** Конвертирует статус платежа в enum REST-ответа платежа. */
    default PaymentResponse.StatusEnum toPaymentStatus(PaymentStatus status) {
        return status == null ? null : PaymentResponse.StatusEnum.valueOf(status.name());
    }

    /** Конвертирует статус платежа в enum REST-ответа реквизитов. */
    default PaymentDetailsResponse.StatusEnum toDetailsStatus(PaymentStatus status) {
        return status == null ? null : PaymentDetailsResponse.StatusEnum.valueOf(status.name());
    }

    /** Конвертирует {@link Instant} в {@link OffsetDateTime} по UTC. */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
