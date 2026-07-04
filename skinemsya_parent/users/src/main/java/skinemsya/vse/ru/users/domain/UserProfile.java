package skinemsya.vse.ru.users.domain;

public record UserProfile(
        long id,
        long userId,
        String paymentDetails,
        String phone,
        String preferredBank,
        String notificationSettings
) {
}
