package skinemsya.vse.ru.subscriptions.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.security.AuthenticatedUser;
import skinemsya.vse.ru.subscriptions.api.dto.SubscriptionCheckoutRequest;
import skinemsya.vse.ru.subscriptions.api.dto.SubscriptionCheckoutResponse;
import skinemsya.vse.ru.subscriptions.api.dto.SubscriptionResponse;
import skinemsya.vse.ru.subscriptions.application.SubscriptionServiceImpl;

@RestController
public class SubscriptionController {

    private final SubscriptionServiceImpl subscriptionService;

    public SubscriptionController(SubscriptionServiceImpl subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/api/v1/me/subscription")
    public SubscriptionResponse current(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        long userId = requireUserId(authenticatedUser);
        subscriptionService.syncPendingYooKassa(userId);
        return SubscriptionResponse.from(subscriptionService.current(userId));
    }

    @PostMapping("/api/v1/me/subscription/checkout")
    public SubscriptionCheckoutResponse checkout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody SubscriptionCheckoutRequest request) {
        return SubscriptionCheckoutResponse.from(subscriptionService.checkout(
                requireUserId(authenticatedUser), request.method(), Boolean.TRUE.equals(request.sbp())));
    }

    @PostMapping("/api/v1/me/subscription/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        subscriptionService.cancel(requireUserId(authenticatedUser));
    }

    private static long requireUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "User is not authenticated");
        }
        return authenticatedUser.getUserId();
    }
}
