package skinemsya.vse.ru.impl.mapper.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createDebt;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createDebtSummary;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createPaymentInfo;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import skinemsya.vse.ru.api.rest.debts.dto.DebtResponse;

/** Тесты MapStruct-маппинга REST DTO долгов. */
class DebtRestMapperTest {

    private final DebtRestMapper mapper = Mappers.getMapper(DebtRestMapper.class);

    /** Проверяет преобразование долга и платежа в REST-ответ с датами в UTC. */
    @Test
    void shouldMapDebtToResponse() {
        var response = mapper.toResponse(createDebt(), createPaymentInfo());

        assertThat(response.getId()).isEqualTo(40L);
        assertThat(response.getStatus()).isEqualTo(DebtResponse.StatusEnum.UNPAID);
        assertThat(response.getPaymentStatus()).isEqualTo("DEBTOR_CONFIRMED");
        assertThat(response.getScreenshotFileId()).isEqualTo(70L);
        assertThat(response.getCreatedAt())
                .isEqualTo(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Оставляет поля платежа пустыми, если платежа ещё нет. */
    @Test
    void shouldMapDebtWithoutPayment() {
        var response = mapper.toResponse(createDebt(), null);

        assertThat(response.getPaymentStatus()).isNull();
        assertThat(response.getScreenshotFileId()).isNull();
    }

    /** Проверяет преобразование сводки долгов. */
    @Test
    void shouldMapSummaryToResponse() {
        var response = mapper.toResponse(createDebtSummary());

        assertThat(response.getTotalOwedKopecks()).isEqualTo(5_000L);
        assertThat(response.getPendingCount()).isZero();
    }

    /** Проверяет преобразование Instant в OffsetDateTime по UTC. */
    @Test
    void shouldMapInstantToOffsetDateTime() {
        assertThat(mapper.toOffsetDateTime(Instant.parse("2026-01-01T10:00:00Z")))
                .isEqualTo(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        assertThat(mapper.toOffsetDateTime(null)).isNull();
    }
}
