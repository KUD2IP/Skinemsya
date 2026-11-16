package skinemsya.vse.ru.api.internal.pojo.users;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Страница пользователей в результатах поиска. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSlice {

    private List<User> items;
    private int page;
    private int size;
    private long total;
}
