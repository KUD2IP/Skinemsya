package skinemsya.vse.ru.impl.service.auth;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.impl.support.util.RefreshTokenHasher;
import skinemsya.vse.ru.api.internal.pojo.auth.IssuedRefreshToken;
import skinemsya.vse.ru.api.internal.pojo.auth.RefreshToken;
import skinemsya.vse.ru.api.internal.repository.auth.RefreshTokenRepository;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.impl.support.properties.JwtProperties;

/** Выдача, ротация и отзыв refresh-токенов. */
@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /** Выдаёт новый refresh-токен пользователю. */
    public IssuedRefreshToken issue(long userId) {
        var rawToken = UUID.randomUUID().toString();
        var token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTtl()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(token);
        return new IssuedRefreshToken(rawToken, userId);
    }

    /** Отзывает все действующие refresh-токены пользователя. */
    public void revokeAllActiveForUser(long userId) {
        var activeTokens = refreshTokenRepository.findActiveByUserId(userId, Instant.now());
        activeTokens.forEach(token -> token.setRevoked(true));
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }
    }

    /** Отзывает текущий токен и выдаёт замену. */
    public IssuedRefreshToken rotate(String rawRefreshToken) {
        var token = findActiveToken(rawRefreshToken);
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return issue(token.getUserId());
    }

    /** Проверяет refresh-токен и возвращает владельца. */
    public long validateAndGetUserId(String rawRefreshToken) {
        return findActiveToken(rawRefreshToken).getUserId();
    }

    /** Отзывает refresh-токен, если он существует и ещё действует. */
    public void revoke(String rawRefreshToken) {
        var token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).orElse(null);
        if (token != null && !token.isRevoked()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
    }

    private RefreshToken findActiveToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Refresh token is missing");
        }
        var token = refreshTokenRepository
                .findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid refresh token"));
        if (token.isRevoked()) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Refresh token is revoked");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Refresh token is expired");
        }
        return token;
    }

    /** Вычисляет SHA-256 хеш исходного токена. */
    public static String hash(String rawToken) {
        return RefreshTokenHasher.hash(rawToken);
    }
}
