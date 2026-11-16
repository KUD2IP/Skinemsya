package skinemsya.vse.ru.impl.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.data.entity.factory.EntityFactory.createRefreshTokenEntity;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга refresh-токенов. */
class RefreshTokenEntityMapperTest {

    private final RefreshTokenEntityMapper mapper = Mappers.getMapper(RefreshTokenEntityMapper.class);

    /** Проверяет преобразование refresh-токена в обе стороны без потери полей. */
    @Test
    void shouldMapRefreshTokenInBothDirections() {
        var entity = createRefreshTokenEntity("refresh-token");

        var token = mapper.toRefreshToken(entity);
        var mappedEntity = mapper.toEntity(token);

        assertThat(token.getUserId()).isEqualTo(1L);
        assertThat(mappedEntity).usingRecursiveComparison().isEqualTo(entity);
    }
}
