package skinemsya.vse.ru.users.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 2000) String paymentDetails,
        @Size(max = 20) String phone,
        @Size(max = 100) String preferredBank,
        String notificationSettings
) {
}
