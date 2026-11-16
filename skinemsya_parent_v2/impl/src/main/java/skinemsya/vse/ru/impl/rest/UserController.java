package skinemsya.vse.ru.impl.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.api.rest.users.dto.UpdateProfileRequest;
import skinemsya.vse.ru.api.rest.users.dto.UserResponse;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.impl.mapper.rest.UserRestMapper;

/** REST API текущего пользователя. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRestMapper userRestMapper;

    /** Возвращает текущего пользователя и его профиль. */
    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        long userId = requireUserId(authenticatedUser);
        var user = userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        var profile = userService.getProfile(userId);
        return userRestMapper.toResponse(user, profile);
    }

    /** Обновляет профиль текущего пользователя. */
    @PutMapping("/me/profile")
    public UserResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        long userId = requireUserId(authenticatedUser);
        var user = userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        var profile = userService.updateProfile(
                userId,
                request.getPaymentDetails(),
                request.getPhone(),
                request.getPreferredBank(),
                request.getNotificationSettings());
        return userRestMapper.toResponse(user, profile);
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
