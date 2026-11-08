package skinemsya.vse.ru.subscriptions.application;

import org.springframework.stereotype.Component;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.subscriptions.infrastructure.config.AdminProperties;
import skinemsya.vse.ru.users.application.UserService;

@Component
public class AdminAccess {

    private final AdminProperties properties;
    private final UserService userService;

    public AdminAccess(AdminProperties properties, UserService userService) {
        this.properties = properties;
        this.userService = userService;
    }

    public void require(long userId) {
        var user = userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        if (!properties.matches(user.telegramUsername())) {
            throw new DomainException(ErrorCode.AUTHORIZATION_ERROR, "Недостаточно прав");
        }
    }
}
