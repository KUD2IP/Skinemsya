package skinemsya.vse.ru.auth.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.auth.api.dto.ChatBootstrapResponse;
import skinemsya.vse.ru.auth.api.dto.RefreshTokenRequest;
import skinemsya.vse.ru.auth.api.dto.TelegramAuthRequest;
import skinemsya.vse.ru.auth.api.dto.TokenResponse;
import skinemsya.vse.ru.auth.application.AuthService;
import skinemsya.vse.ru.auth.domain.AuthResult;
import skinemsya.vse.ru.auth.domain.TokenPair;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/telegram")
    public TokenResponse authenticate(@Valid @RequestBody TelegramAuthRequest request) {
        return toResponse(authService.authenticate(request.initData()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokenPair = authService.refresh(request.refreshToken());
        return toResponse(tokenPair);
    }

    private static TokenResponse toResponse(AuthResult authResult) {
        var chatBootstrap = authResult.chatBootstrap()
                .map(bootstrap -> new ChatBootstrapResponse(
                        bootstrap.groupId(),
                        bootstrap.groupName(),
                        bootstrap.groupType(),
                        bootstrap.suggestedAction(),
                        bootstrap.eventId()
                ))
                .orElse(null);
        return toResponse(authResult.tokens(), chatBootstrap);
    }

    private static TokenResponse toResponse(TokenPair tokenPair) {
        return toResponse(tokenPair, null);
    }

    private static TokenResponse toResponse(TokenPair tokenPair, ChatBootstrapResponse chatBootstrap) {
        return new TokenResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresInSeconds(),
                chatBootstrap
        );
    }
}
