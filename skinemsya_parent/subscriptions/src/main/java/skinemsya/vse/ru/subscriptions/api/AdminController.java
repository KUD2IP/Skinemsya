package skinemsya.vse.ru.subscriptions.api;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.subscriptions.api.dto.AdminUserResponse;
import skinemsya.vse.ru.subscriptions.api.dto.AdminUsersPageResponse;
import skinemsya.vse.ru.subscriptions.application.AdminAccess;
import skinemsya.vse.ru.subscriptions.application.AdminActivityQuery;
import skinemsya.vse.ru.subscriptions.application.AdminActivityQuery.AdminActivity;
import skinemsya.vse.ru.subscriptions.application.SubscriptionServiceImpl;
import skinemsya.vse.ru.subscriptions.infrastructure.config.SubscriptionProperties;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.User;

@RestController
public class AdminController {

    private final AdminAccess adminAccess;
    private final UserService userService;
    private final SubscriptionServiceImpl subscriptionService;
    private final SubscriptionProperties subscriptionProperties;
    private final AdminActivityQuery adminActivityQuery;

    public AdminController(
            AdminAccess adminAccess,
            UserService userService,
            SubscriptionServiceImpl subscriptionService,
            SubscriptionProperties subscriptionProperties,
            AdminActivityQuery adminActivityQuery) {
        this.adminAccess = adminAccess;
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.subscriptionProperties = subscriptionProperties;
        this.adminActivityQuery = adminActivityQuery;
    }

    @GetMapping("/api/v1/admin/me")
    public AdminMe me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        adminAccess.require(requireUserId(authenticatedUser));
        return new AdminMe(true);
    }

    @GetMapping("/api/v1/admin/users")
    public AdminUsersPageResponse users(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        long adminId = requireUserId(authenticatedUser);
        adminAccess.require(adminId);
        var slice = userService.searchOthers(adminId, q, page, size);
        Map<Long, AdminActivity> activity =
                adminActivityQuery.forUsers(slice.items().stream().map(User::id).toList());
        return new AdminUsersPageResponse(
                slice.items().stream()
                        .map(user -> toResponse(user, activity.get(user.id())))
                        .toList(),
                slice.page(),
                slice.size(),
                slice.total());
    }

    @PostMapping("/api/v1/admin/users/{userId}/subscription")
    public AdminUserResponse grant(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long userId) {
        adminAccess.require(requireUserId(authenticatedUser));
        subscriptionService.grantDays(userId, subscriptionProperties.periodDays());
        User user = userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        return toResponse(user, adminActivityQuery.forUsers(List.of(user.id())).get(user.id()));
    }

    @DeleteMapping("/api/v1/admin/users/{userId}/subscription")
    public AdminUserResponse revoke(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser, @PathVariable long userId) {
        adminAccess.require(requireUserId(authenticatedUser));
        subscriptionService.revoke(userId);
        User user = userService
                .findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "User not found"));
        return toResponse(user, adminActivityQuery.forUsers(List.of(user.id())).get(user.id()));
    }

    private AdminUserResponse toResponse(User user, AdminActivity activity) {
        var subscription = subscriptionService.current(user.id());
        AdminActivity stats = activity == null ? new AdminActivity(0, 0) : activity;
        return new AdminUserResponse(
                user.id(),
                user.displayName(),
                user.telegramUsername(),
                user.createdAt(),
                subscription.currentPeriodEnd(),
                subscription.active() && subscription.currentPeriodEnd() != null,
                subscription.provider() == null ? null : subscription.provider().name(),
                stats.eventsCreated(),
                stats.receiptCount());
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }

    public record AdminMe(boolean admin) {}
}
