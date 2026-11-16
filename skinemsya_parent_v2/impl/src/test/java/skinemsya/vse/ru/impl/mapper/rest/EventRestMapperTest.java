package skinemsya.vse.ru.impl.mapper.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.rest.events.dto.EventResponse;

/** Тесты MapStruct-маппинга REST DTO сборов. */
class EventRestMapperTest {

    private final EventRestMapper mapper = Mappers.getMapper(EventRestMapper.class);

    /** Проверяет преобразование сбора в REST-ответ с датами в UTC и вычисленными полями. */
    @Test
    void shouldMapEventToResponse() {
        var response = mapper.toResponse(createEvent(), true, 2L, true);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(EventResponse.StatusEnum.DRAFT);
        assertThat(response.getPayerRequisitesReady()).isTrue();
        assertThat(response.getJoinedCount()).isEqualTo(2L);
        assertThat(response.getCurrentUserJoined()).isTrue();
        assertThat(response.getCreatedAt())
                .isEqualTo(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Проверяет преобразование страницы ответов сбора в постраничный REST-ответ. */
    @Test
    void shouldMapEventPageToResponse() {
        var page = new PageResult<>(List.of(mapper.toResponse(createEvent(), false, 1L, false)), 0, 20, 1L);

        var response = mapper.toEventPageResponse(page);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    /** Проверяет преобразование Instant в OffsetDateTime по UTC. */
    @Test
    void shouldMapInstantToOffsetDateTime() {
        assertThat(mapper.toOffsetDateTime(Instant.parse("2026-01-01T10:00:00Z")))
                .isEqualTo(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        assertThat(mapper.toOffsetDateTime(null)).isNull();
    }
}
