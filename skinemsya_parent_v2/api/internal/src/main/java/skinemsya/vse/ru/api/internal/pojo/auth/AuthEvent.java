package skinemsya.vse.ru.api.internal.pojo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Данные события, необходимые при аутентификации. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthEvent {

    private long id;
    private long groupId;
}
