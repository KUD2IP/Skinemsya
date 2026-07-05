package skinemsya.vse.ru.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.auth.infrastructure.config.JwtProperties;
import skinemsya.vse.ru.auth.infrastructure.persistence.RefreshTokenEntity;
import skinemsya.vse.ru.auth.infrastructure.persistence.RefreshTokenRepository;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public IssuedRefreshToken issue(long userId) {
        var rawToken = UUID.randomUUID().toString();
        var entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(Instant.now().plus(jwtProperties.refreshTtl()));
        entity.setRevoked(false);
        entity.setCreatedAt(Instant.now());
        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(rawToken, userId);
    }

    public void revokeAllActiveForUser(long userId) {
        var activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(userId, Instant.now());
        for (var token : activeTokens) {
            token.setRevoked(true);
        }
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }
    }

    public IssuedRefreshToken rotate(String rawRefreshToken) {
        var entity = findActiveToken(rawRefreshToken);
        entity.setRevoked(true);
        refreshTokenRepository.save(entity);
        return issue(entity.getUserId());
    }

    public long validateAndGetUserId(String rawRefreshToken) {
        return findActiveToken(rawRefreshToken).getUserId();
    }

    public void revoke(String rawRefreshToken) {
        var entity =
                refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).orElse(null);
        if (entity != null && !entity.isRevoked()) {
            entity.setRevoked(true);
            refreshTokenRepository.save(entity);
        }
    }

    private RefreshTokenEntity findActiveToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Refresh token is missing");
        }
        var entity = refreshTokenRepository
                .findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid refresh token"));
        if (entity.isRevoked()) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Refresh token is revoked");
        }
        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Refresh token is expired");
        }
        return entity;
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record IssuedRefreshToken(String rawToken, long userId) {}
}
