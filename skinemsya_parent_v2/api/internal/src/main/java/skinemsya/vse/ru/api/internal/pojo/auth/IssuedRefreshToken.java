package skinemsya.vse.ru.api.internal.pojo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Новый refresh-токен и владелец. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssuedRefreshToken {

    private String rawToken;
    private long userId;
}
