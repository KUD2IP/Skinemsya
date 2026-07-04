package skinemsya.vse.ru.users.application;

import skinemsya.vse.ru.users.domain.PaymentDetails;
import skinemsya.vse.ru.users.domain.TelegramUserData;
import skinemsya.vse.ru.users.domain.User;
import skinemsya.vse.ru.users.domain.UserProfile;

import java.util.Optional;

public interface UserService {

    Optional<User> findById(long userId);

    Optional<User> findByTelegramUserId(long telegramUserId);

    Optional<User> findByTelegramUsername(String telegramUsername);

    User upsertFromTelegram(TelegramUserData telegramUserData);

    UserProfile updateProfile(
            long userId,
            String paymentDetails,
            String phone,
            String preferredBank,
            String notificationSettings
    );

    PaymentDetails getPaymentDetails(long userId);

    UserProfile getProfile(long userId);
}
