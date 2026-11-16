package skinemsya.vse.ru.impl.mapper.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createAuthResult;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createTokenPair;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

/** Тесты MapStruct-маппинга REST DTO аутентификации. */
class AuthRestMapperTest {

    private final AuthRestMapper mapper = Mappers.getMapper(AuthRestMapper.class);

    /** Проверяет маппинг результата аутентификации и вложенного chat bootstrap. */
    @Test
    void shouldMapAuthResult() {
        var response = mapper.toResponse(createAuthResult());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        assertThat(response.getChatBootstrap().getGroupId()).isEqualTo(10L);
        assertThat(response.getChatBootstrap().getEventId()).isEqualTo(20L);
    }

    /** Проверяет маппинг обновлённой пары токенов без chat bootstrap. */
    @Test
    void shouldMapTokenPair() {
        var response = mapper.toResponse(createTokenPair());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getChatBootstrap()).isNull();
    }
}
