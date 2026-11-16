package skinemsya.vse.ru.api.internal.service.auth;

import skinemsya.vse.ru.api.internal.pojo.auth.AuthResult;
import skinemsya.vse.ru.api.internal.pojo.auth.TokenPair;

/** Сценарии аутентификации и управления сессией. */
public interface AuthService {

    /** Аутентифицирует пользователя по Telegram initData. */
    AuthResult authenticate(String initData);

    /** Обновляет пару токенов по refresh-токену. */
    TokenPair refresh(String refreshToken);

    /** Отзывает refresh-токен. */
    void revoke(String refreshToken);
}
