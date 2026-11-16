package skinemsya.vse.ru.impl.mapper.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createStoredFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга REST DTO файлов. */
class FileRestMapperTest {

    private final FileRestMapper mapper = Mappers.getMapper(FileRestMapper.class);

    /** Проверяет преобразование файла в REST-ответ с датой в UTC. */
    @Test
    void shouldMapFileToResponse() {
        var response = mapper.toResponse(createStoredFile());

        assertThat(response.getId()).isEqualTo(70L);
        assertThat(response.getMimeType()).isEqualTo("image/jpeg");
        assertThat(response.getCreatedAt())
                .isEqualTo(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    }
}
