package skinemsya.vse.ru.auth.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import skinemsya.vse.ru.auth.infrastructure.config.JwtProperties;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        if (jwtProperties.secret() == null || jwtProperties.secret().length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(long userId) {
        var now = Instant.now();
        var expiresAt = now.plus(jwtProperties.accessTtl());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public long parseUserId(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid access token", ex);
        }
    }

    public long accessTtlSeconds() {
        return jwtProperties.accessTtl().getSeconds();
    }
}
