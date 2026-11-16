package skinemsya.vse.ru.api.internal.pojo.auth;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Результат аутентификации через Telegram. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {

    private TokenPair tokens;
    private Optional<ChatBootstrap> chatBootstrap;
}
