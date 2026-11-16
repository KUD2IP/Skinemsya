package skinemsya.vse.ru.impl.mapper.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createPayment;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createPaymentDetailsView;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentDetailsResponse;
import skinemsya.vse.ru.api.rest.payments.dto.PaymentResponse;

/** Тесты MapStruct-маппинга REST DTO платежей. */
class PaymentRestMapperTest {

    private final PaymentRestMapper mapper = Mappers.getMapper(PaymentRestMapper.class);

    /** Проверяет преобразование платежа в REST-ответ с датами в UTC. */
    @Test
    void shouldMapPaymentToResponse() {
        var response = mapper.toResponse(createPayment());

        assertThat(response.getId()).isEqualTo(60L);
        assertThat(response.getStatus()).isEqualTo(PaymentResponse.StatusEnum.DEBTOR_CONFIRMED);
        assertThat(response.getDebtorConfirmedAt())
                .isEqualTo(OffsetDateTime.of(2026, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Проверяет преобразование реквизитов оплаты в REST-ответ. */
    @Test
    void shouldMapPaymentDetailsToResponse() {
        var response = mapper.toResponse(createPaymentDetailsView());

        assertThat(response.getCreditorName()).isEqualTo("Alice");
        assertThat(response.getAmountKopecks()).isEqualTo(5_000L);
        assertThat(response.getStatus()).isEqualTo(PaymentDetailsResponse.StatusEnum.CREATED);
    }
}
