package skinemsya.vse.ru.api.internal.pojo.users;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Внутреннее представление пользователя. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private long id;
    private long telegramUserId;
    private String displayName;
    private String telegramUsername;
    private Instant createdAt;
    private Instant updatedAt;
    private String photoUrl;
}
