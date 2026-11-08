package skinemsya.vse.ru.users.application;

import java.util.Optional;
import skinemsya.vse.ru.users.domain.PaymentDetails;
import skinemsya.vse.ru.users.domain.TelegramUserData;
import skinemsya.vse.ru.users.domain.User;
import skinemsya.vse.ru.users.domain.UserProfile;

public interface UserService {

    Optional<User> findById(long userId);

    Optional<User> findByTelegramUserId(long telegramUserId);

    Optional<User> findByTelegramUsername(String telegramUsername);

    UserSlice searchOthers(long excludeUserId, String usernameQuery, int page, int size);

    User upsertFromTelegram(TelegramUserData telegramUserData);

    UserProfile updateProfile(
            long userId, String paymentDetails, String phone, String preferredBank, String notificationSettings);

    PaymentDetails getPaymentDetails(long userId);

    UserProfile getProfile(long userId);
}
