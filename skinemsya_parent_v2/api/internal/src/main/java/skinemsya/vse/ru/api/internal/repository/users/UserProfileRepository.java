package skinemsya.vse.ru.api.internal.repository.users;

import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;

/** Порт хранения профилей пользователей. */
public interface UserProfileRepository {

    /** Находит профиль по идентификатору пользователя. */
    Optional<UserProfile> findByUserId(long userId);

    /** Атомарно создаёт пустой профиль, если его ещё нет. */
    void insertIfAbsent(long userId);

    /** Сохраняет профиль пользователя. */
    UserProfile save(UserProfile profile);
}
