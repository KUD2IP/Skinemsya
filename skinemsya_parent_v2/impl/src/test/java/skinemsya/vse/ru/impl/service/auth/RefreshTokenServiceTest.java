package skinemsya.vse.ru.impl.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createRefreshToken;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.api.internal.pojo.auth.RefreshToken;
import skinemsya.vse.ru.api.internal.repository.auth.RefreshTokenRepository;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.impl.support.properties.JwtProperties;

/** Тесты жизненного цикла refresh-токенов. */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    /** Создаёт сервис с тестовыми настройками. */
    @BeforeEach
    void setUp() {
        var jwtProperties = new JwtProperties(
                "test-jwt-secret-at-least-32-characters-long", Duration.ofMinutes(15), Duration.ofDays(7));
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties);
    }

    /** Проверяет выдачу refresh-токена и сохранение только его хеша. */
    @Test
    void shouldIssueRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var issued = refreshTokenService.issue(1L);

        assertThat(issued.getRawToken()).isNotBlank();
        assertThat(issued.getUserId()).isEqualTo(1L);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(issued.getRawToken()));
    }

    /** Проверяет запрет ротации отозванного токена. */
    @Test
    void shouldRejectRevokedRefreshToken() {
        var rawToken = "revoked-token";
        var token = createRefreshToken(rawToken);
        token.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken)).isInstanceOf(DomainException.class);
    }

    /** Проверяет отзыв всех действующих токенов пользователя. */
    @Test
    void shouldRevokeAllActiveTokensForUser() {
        var first = createRefreshToken("token-one");
        var second = createRefreshToken("token-two");
        when(refreshTokenRepository.findActiveByUserId(eq(1L), any(Instant.class)))
                .thenReturn(List.of(first, second));
        when(refreshTokenRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revokeAllActiveForUser(1L);

        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(any());
    }
}
