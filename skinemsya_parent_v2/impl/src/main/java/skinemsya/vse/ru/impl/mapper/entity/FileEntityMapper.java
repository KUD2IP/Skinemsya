package skinemsya.vse.ru.impl.mapper.entity;

import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;
import skinemsya.vse.ru.data.entity.files.FileEntity;

/** MapStruct-маппер между сущностью файла и внутренним POJO. */
@Mapper(componentModel = "spring")
public interface FileEntityMapper {

    /** Конвертирует сущность файла во внутренний POJO. */
    StoredFile toStoredFile(FileEntity entity);

    /** Конвертирует внутренний POJO файла в сущность. */
    FileEntity toEntity(StoredFile file);
}
