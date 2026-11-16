package skinemsya.vse.ru.impl.mapper.rest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import skinemsya.vse.ru.api.internal.pojo.auth.AuthResult;
import skinemsya.vse.ru.api.internal.pojo.auth.ChatBootstrap;
import skinemsya.vse.ru.api.internal.pojo.auth.TokenPair;
import skinemsya.vse.ru.api.rest.auth.dto.ChatBootstrapResponse;
import skinemsya.vse.ru.api.rest.auth.dto.TokenResponse;

/** MapStruct-маппер внутренних POJO аутентификации в REST DTO. */
@Mapper(componentModel = "spring")
public interface AuthRestMapper {

    /** Преобразует пару токенов в ответ без chat bootstrap. */
    @Mapping(target = "expiresIn", source = "expiresInSeconds")
    @Mapping(target = "chatBootstrap", ignore = true)
    TokenResponse toResponse(TokenPair tokenPair);

    /** Преобразует результат аутентификации в REST DTO. */
    @Mapping(target = "accessToken", source = "tokens.accessToken")
    @Mapping(target = "refreshToken", source = "tokens.refreshToken")
    @Mapping(target = "expiresIn", source = "tokens.expiresInSeconds")
    TokenResponse toResponse(AuthResult authResult);

    /** Разворачивает optional chat bootstrap для MapStruct. */
    default ChatBootstrapResponse toResponse(java.util.Optional<ChatBootstrap> chatBootstrap) {
        return chatBootstrap.map(this::toResponse).orElse(null);
    }

    /** Преобразует chat bootstrap во вложенный REST DTO. */
    ChatBootstrapResponse toResponse(ChatBootstrap chatBootstrap);
}
