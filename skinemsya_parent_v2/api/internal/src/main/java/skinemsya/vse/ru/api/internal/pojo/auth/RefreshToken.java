package skinemsya.vse.ru.api.internal.pojo.auth;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Внутреннее представление refresh-токена. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    private long id;
    private long userId;
    private String tokenHash;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;
}
