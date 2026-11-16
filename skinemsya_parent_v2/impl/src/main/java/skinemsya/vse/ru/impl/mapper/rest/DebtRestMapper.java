package skinemsya.vse.ru.impl.mapper.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;
import skinemsya.vse.ru.api.internal.pojo.debts.Debt;
import skinemsya.vse.ru.api.internal.pojo.debts.DebtSummary;
import skinemsya.vse.ru.api.internal.pojo.payments.PaymentInfo;
import skinemsya.vse.ru.api.rest.debts.dto.DebtResponse;
import skinemsya.vse.ru.api.rest.debts.dto.DebtSummaryResponse;

/** MapStruct-маппер между внутренними POJO долгов и REST DTO. */
@Mapper(componentModel = "spring")
public interface DebtRestMapper {

    /** Конвертирует долг в REST-ответ. */
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "screenshotFileId", ignore = true)
    DebtResponse toResponse(Debt debt);

    /** Конвертирует долг и сведения о платеже в REST-ответ. */
    default DebtResponse toResponse(Debt debt, PaymentInfo paymentInfo) {
        var response = toResponse(debt);
        if (paymentInfo == null) {
            return response;
        }
        return response.paymentStatus(paymentInfo.getStatus()).screenshotFileId(paymentInfo.getScreenshotFileId());
    }

    /** Конвертирует сводку долгов в REST-ответ. */
    DebtSummaryResponse toResponse(DebtSummary summary);

    /** Конвертирует статус долга в enum REST DTO. */
    default DebtResponse.StatusEnum toStatusEnum(DebtStatus status) {
        return status == null ? null : DebtResponse.StatusEnum.valueOf(status.name());
    }

    /** Конвертирует {@link Instant} в {@link OffsetDateTime} по UTC. */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
