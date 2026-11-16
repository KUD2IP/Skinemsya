package skinemsya.vse.ru.impl.mapper.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createGroup;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createGroupMemberView;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.rest.groups.dto.GroupMemberViewResponse;
import skinemsya.vse.ru.api.rest.groups.dto.GroupResponse;

/** Тесты MapStruct-маппинга REST DTO групп. */
class GroupRestMapperTest {

    private final GroupRestMapper mapper = Mappers.getMapper(GroupRestMapper.class);

    /** Проверяет преобразование группы в REST-ответ с датами в UTC. */
    @Test
    void shouldMapGroupToResponse() {
        var response = mapper.toResponse(createGroup());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getType()).isEqualTo(GroupResponse.TypeEnum.STANDALONE);
        assertThat(response.getCreatedAt())
                .isEqualTo(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Проверяет преобразование участника группы в REST-ответ. */
    @Test
    void shouldMapMemberViewToResponse() {
        var response = mapper.toResponse(createGroupMemberView());

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo(GroupMemberViewResponse.RoleEnum.OWNER);
        assertThat(response.getJoinedAt())
                .isEqualTo(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    /** Проверяет преобразование страницы групп в постраничный REST-ответ. */
    @Test
    void shouldMapGroupPageToResponse() {
        var page = new PageResult<>(List.of(createGroup()), 0, 20, 1L);

        var response = mapper.toGroupPageResponse(page);

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
