package skinemsya.vse.ru.impl.mapper.entity;

import java.util.List;
import org.mapstruct.Mapper;
import skinemsya.vse.ru.api.internal.pojo.auth.RefreshToken;
import skinemsya.vse.ru.data.entity.auth.RefreshTokenEntity;

/** MapStruct-маппер refresh-токенов между POJO и JPA. */
@Mapper(componentModel = "spring")
public interface RefreshTokenEntityMapper {

    /** Преобразует JPA-сущность во внутренний POJO. */
    RefreshToken toRefreshToken(RefreshTokenEntity entity);

    /** Преобразует список JPA-сущностей во внутренние POJO. */
    List<RefreshToken> toRefreshTokens(List<RefreshTokenEntity> entities);

    /** Преобразует внутренний POJO в JPA-сущность. */
    RefreshTokenEntity toEntity(RefreshToken token);

    /** Преобразует список внутренних POJO в JPA-сущности. */
    List<RefreshTokenEntity> toEntities(List<RefreshToken> tokens);
}
