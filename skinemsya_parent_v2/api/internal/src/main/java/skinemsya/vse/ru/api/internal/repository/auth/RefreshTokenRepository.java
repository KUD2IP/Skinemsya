package skinemsya.vse.ru.api.internal.repository.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.auth.RefreshToken;

/** Порт хранения refresh-токенов. */
public interface RefreshTokenRepository {

    /** Находит токен по SHA-256 хешу. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Находит действующие токены пользователя. */
    List<RefreshToken> findActiveByUserId(long userId, Instant now);

    /** Сохраняет refresh-токен. */
    RefreshToken save(RefreshToken token);

    /** Сохраняет список refresh-токенов. */
    List<RefreshToken> saveAll(List<RefreshToken> tokens);
}
