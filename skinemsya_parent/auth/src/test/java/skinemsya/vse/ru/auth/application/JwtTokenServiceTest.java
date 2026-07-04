package skinemsya.vse.ru.auth.application;

import org.junit.jupiter.api.Test;
import skinemsya.vse.ru.auth.infrastructure.config.JwtProperties;
import skinemsya.vse.ru.common.domain.DomainException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "test-jwt-secret-at-least-32-characters-long";

    private final JwtTokenService jwtTokenService = new JwtTokenService(
            new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7))
    );

    @Test
    void shouldCreateAndParseAccessToken() {
        var token = jwtTokenService.createAccessToken(42L);

        assertThat(jwtTokenService.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void shouldRejectInvalidAccessToken() {
        assertThatThrownBy(() -> jwtTokenService.parseUserId("invalid.token.value"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRejectShortSecret() {
        assertThatThrownBy(() -> new JwtTokenService(
                new JwtProperties("short", Duration.ofMinutes(15), Duration.ofDays(7))
        )).isInstanceOf(IllegalStateException.class);
    }
}
