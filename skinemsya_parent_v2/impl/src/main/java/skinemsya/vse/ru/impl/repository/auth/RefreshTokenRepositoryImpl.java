package skinemsya.vse.ru.impl.repository.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import skinemsya.vse.ru.api.internal.pojo.auth.RefreshToken;
import skinemsya.vse.ru.api.internal.repository.auth.RefreshTokenRepository;
import skinemsya.vse.ru.impl.mapper.entity.RefreshTokenEntityMapper;

/** Адаптер между внутренним портом refresh-токенов и Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final RefreshTokenEntityMapper refreshTokenEntityMapper;

    /** {@inheritDoc} */
    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(refreshTokenEntityMapper::toRefreshToken);
    }

    /** {@inheritDoc} */
    @Override
    public List<RefreshToken> findActiveByUserId(long userId, Instant now) {
        var entities = refreshTokenJpaRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(userId, now);
        return refreshTokenEntityMapper.toRefreshTokens(entities);
    }

    /** {@inheritDoc} */
    @Override
    public RefreshToken save(RefreshToken token) {
        var saved = refreshTokenJpaRepository.save(refreshTokenEntityMapper.toEntity(token));
        return refreshTokenEntityMapper.toRefreshToken(saved);
    }

    /** {@inheritDoc} */
    @Override
    public List<RefreshToken> saveAll(List<RefreshToken> tokens) {
        var saved = refreshTokenJpaRepository.saveAll(refreshTokenEntityMapper.toEntities(tokens));
        return refreshTokenEntityMapper.toRefreshTokens(saved);
    }
}
