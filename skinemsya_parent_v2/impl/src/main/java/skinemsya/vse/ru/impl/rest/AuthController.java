package skinemsya.vse.ru.impl.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinemsya.vse.ru.api.internal.service.auth.AuthService;
import skinemsya.vse.ru.api.rest.auth.dto.RefreshTokenRequest;
import skinemsya.vse.ru.api.rest.auth.dto.TelegramAuthRequest;
import skinemsya.vse.ru.api.rest.auth.dto.TokenResponse;
import skinemsya.vse.ru.impl.mapper.rest.AuthRestMapper;

/** REST API аутентификации и обновления токенов. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthRestMapper authRestMapper;

    /** Аутентифицирует пользователя по Telegram initData. */
    @PostMapping("/telegram")
    public TokenResponse authenticate(@Valid @RequestBody TelegramAuthRequest request) {
        return authRestMapper.toResponse(authService.authenticate(request.getInitData()));
    }

    /** Обновляет access и refresh токены. */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authRestMapper.toResponse(authService.refresh(request.getRefreshToken()));
    }
}
