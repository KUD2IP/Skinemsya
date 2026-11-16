package skinemsya.vse.ru.impl.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createFileEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга файлов. */
class FileEntityMapperTest {

    private final FileEntityMapper mapper = Mappers.getMapper(FileEntityMapper.class);

    /** Проверяет преобразование файла в обе стороны без потери полей. */
    @Test
    void shouldMapFileInBothDirections() {
        var entity = createFileEntity();

        var file = mapper.toStoredFile(entity);
        var mappedEntity = mapper.toEntity(file);

        assertThat(file.getOriginalName()).isEqualTo("receipt.jpg");
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }
}
