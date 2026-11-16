package skinemsya.vse.ru.impl.repository.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import skinemsya.vse.ru.data.entity.auth.RefreshTokenEntity;

/** Spring Data репозиторий refresh-токенов. */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /** Находит сущность по хешу токена. */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /** Находит действующие сущности пользователя. */
    List<RefreshTokenEntity> findByUserIdAndRevokedFalseAndExpiresAtAfter(long userId, Instant now);
}
