package skinemsya.vse.ru.api.internal.pojo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.groups.GroupType;

/** Данные группы, необходимые при аутентификации. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthGroup {

    private long id;
    private String name;
    private GroupType type;
}
