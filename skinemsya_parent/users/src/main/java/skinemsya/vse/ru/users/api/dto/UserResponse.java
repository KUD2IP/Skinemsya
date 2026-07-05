package skinemsya.vse.ru.users.api.dto;

public record UserResponse(
        long id,
        long telegramUserId,
        String displayName,
        String paymentDetails,
        String phone,
        String preferredBank,
        String notificationSettings) {}
