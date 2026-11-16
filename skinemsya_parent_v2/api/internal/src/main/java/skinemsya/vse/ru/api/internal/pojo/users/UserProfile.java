package skinemsya.vse.ru.api.internal.pojo.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Внутреннее представление профиля пользователя. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    private long id;
    private long userId;
    private String paymentDetails;
    private String phone;
    private String preferredBank;
    private String notificationSettings;
}
