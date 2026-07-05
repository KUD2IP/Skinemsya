package skinemsya.vse.ru.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.auth.infrastructure.config.JwtProperties;
import skinemsya.vse.ru.auth.infrastructure.persistence.RefreshTokenEntity;
import skinemsya.vse.ru.auth.infrastructure.persistence.RefreshTokenRepository;
import skinemsya.vse.ru.common.domain.DomainException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private final JwtProperties jwtProperties = new JwtProperties(
            "test-jwt-secret-at-least-32-characters-long", Duration.ofMinutes(15), Duration.ofDays(7));

    @Test
    void shouldIssueRefreshToken() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> {
            RefreshTokenEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        var issued = refreshTokenService.issue(10L);

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.userId()).isEqualTo(10L);

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(issued.rawToken()));
    }

    @Test
    void shouldRejectRevokedRefreshToken() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties);
        var rawToken = "revoked-token";
        var entity = activeEntity(rawToken);
        entity.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(rawToken)))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken)).isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRevokeAllActiveTokensForUser() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties);
        var first = activeEntity("token-one");
        var second = activeEntity("token-two");
        when(refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(eq(10L), any(Instant.class)))
                .thenReturn(List.of(first, second));
        when(refreshTokenRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revokeAllActiveForUser(10L);

        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(any());
    }

    private RefreshTokenEntity activeEntity(String rawToken) {
        var entity = new RefreshTokenEntity();
        entity.setId(1L);
        entity.setUserId(10L);
        entity.setTokenHash(RefreshTokenService.hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        entity.setRevoked(false);
        entity.setCreatedAt(Instant.now());
        return entity;
    }
}
