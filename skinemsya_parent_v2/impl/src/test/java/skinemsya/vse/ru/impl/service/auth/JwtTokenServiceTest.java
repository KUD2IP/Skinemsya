package skinemsya.vse.ru.impl.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.impl.support.properties.JwtProperties;

/** Тесты создания и проверки JWT. */
class JwtTokenServiceTest {

    private static final String SECRET = "test-jwt-secret-at-least-32-characters-long";

    private final JwtTokenService jwtTokenService =
            new JwtTokenService(new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7)));

    /** Проверяет создание и чтение access-токена. */
    @Test
    void shouldCreateAndParseAccessToken() {
        var token = jwtTokenService.createAccessToken(42L);

        assertThat(jwtTokenService.parseUserId(token)).isEqualTo(42L);
    }

    /** Проверяет отклонение некорректного access-токена. */
    @Test
    void shouldRejectInvalidAccessToken() {
        assertThatThrownBy(() -> jwtTokenService.parseUserId("invalid.token.value"))
                .isInstanceOf(DomainException.class);
    }

    /** Проверяет запрет короткого JWT-секрета. */
    @Test
    void shouldRejectShortSecret() {
        assertThatThrownBy(() ->
                        new JwtTokenService(new JwtProperties("short", Duration.ofMinutes(15), Duration.ofDays(7))))
                .isInstanceOf(IllegalStateException.class);
    }
}
