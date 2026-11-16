package skinemsya.vse.ru.impl.mapper.rest;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;
import skinemsya.vse.ru.api.rest.files.dto.FileResponse;

/** MapStruct-маппер между внутренним POJO файла и REST DTO. */
@Mapper(componentModel = "spring")
public interface FileRestMapper {

    /** Конвертирует файл в REST-ответ. */
    FileResponse toResponse(StoredFile file);

    /** Конвертирует {@link Instant} в {@link OffsetDateTime} по UTC. */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
