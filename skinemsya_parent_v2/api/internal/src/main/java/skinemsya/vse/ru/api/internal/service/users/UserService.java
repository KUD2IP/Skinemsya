package skinemsya.vse.ru.api.internal.service.users;

import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.users.PaymentDetails;
import skinemsya.vse.ru.api.internal.pojo.users.TelegramUserData;
import skinemsya.vse.ru.api.internal.pojo.users.User;
import skinemsya.vse.ru.api.internal.pojo.users.UserProfile;
import skinemsya.vse.ru.api.internal.pojo.users.UserSlice;

/** Операции с пользователями и их профилями. */
public interface UserService {

    /** Находит пользователя по внутреннему идентификатору. */
    Optional<User> findById(long userId);

    /** Находит пользователя по идентификатору Telegram. */
    Optional<User> findByTelegramUserId(long telegramUserId);

    /** Находит пользователя по имени в Telegram. */
    Optional<User> findByTelegramUsername(String telegramUsername);

    /** Ищет пользователей, исключая текущего. */
    UserSlice searchOthers(long excludeUserId, String usernameQuery, int page, int size);

    /** Создаёт пользователя или обновляет его данные из Telegram. */
    User upsertFromTelegram(TelegramUserData telegramUserData);

    /** Обновляет заполненные поля профиля пользователя. */
    UserProfile updateProfile(
            long userId, String paymentDetails, String phone, String preferredBank, String notificationSettings);

    /** Возвращает платёжные реквизиты пользователя. */
    PaymentDetails getPaymentDetails(long userId);

    /** Возвращает профиль пользователя. */
    UserProfile getProfile(long userId);
}
