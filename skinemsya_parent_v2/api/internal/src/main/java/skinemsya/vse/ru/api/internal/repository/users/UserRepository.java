package skinemsya.vse.ru.api.internal.repository.users;

import java.time.Instant;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserSlice;

/** Порт хранения пользователей. */
public interface UserRepository {

    /** Находит пользователя по внутреннему идентификатору. */
    Optional<User> findById(long userId);

    /** Находит пользователя по идентификатору Telegram. */
    Optional<User> findByTelegramUserId(long telegramUserId);

    /** Находит пользователя по нормализованному имени Telegram. */
    Optional<User> findByTelegramUsername(String telegramUsername);

    /** Возвращает страницу пользователей без исключённого идентификатора. */
    UserSlice findAllExcept(long excludeUserId, int page, int size);

    /** Ищет страницу пользователей по фрагменту имени Telegram. */
    UserSlice findByUsernameExcept(long excludeUserId, String usernameQuery, int page, int size);

    /** Атомарно создаёт пользователя Telegram, если его ещё нет. */
    void insertTelegramUserIfAbsent(
            long telegramUserId, String displayName, String telegramUsername, Instant createdAt, Instant updatedAt);

    /** Удаляет дублирующееся имя Telegram у остальных пользователей. */
    void clearTelegramUsernameForOtherUsers(String telegramUsername, long telegramUserId, Instant updatedAt);

    /** Сохраняет пользователя. */
    User save(User user);
}
