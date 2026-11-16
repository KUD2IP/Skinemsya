package skinemsya.vse.ru.api.internal.pojo.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Пара access и refresh токенов. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair {

    private String accessToken;
    private String refreshToken;
    private long expiresInSeconds;
}
