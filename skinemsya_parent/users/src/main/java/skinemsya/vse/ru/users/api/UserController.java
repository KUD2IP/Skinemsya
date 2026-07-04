package skinemsya.vse.ru.users.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.users.api.dto.UpdateProfileRequest;
import skinemsya.vse.ru.users.api.dto.UserResponse;
import skinemsya.vse.ru.users.application.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        long userId = requireUserId(authenticatedUser);
        var user = userService.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        var profile = userService.getProfile(userId);
        return new UserResponse(
                user.id(),
                user.telegramUserId(),
                user.displayName(),
                profile.paymentDetails(),
                profile.phone(),
                profile.notificationSettings()
        );
    }

    @PutMapping("/me/profile")
    public UserResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        long userId = requireUserId(authenticatedUser);
        var user = userService.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        var profile = userService.updateProfile(
                userId,
                request.paymentDetails(),
                request.phone(),
                request.notificationSettings()
        );
        return new UserResponse(
                user.id(),
                user.telegramUserId(),
                user.displayName(),
                profile.paymentDetails(),
                profile.phone(),
                profile.notificationSettings()
        );
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
