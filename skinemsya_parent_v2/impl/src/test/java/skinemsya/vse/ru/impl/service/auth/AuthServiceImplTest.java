package skinemsya.vse.ru.impl.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createTelegramInitData;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.api.internal.pojo.auth.IssuedRefreshToken;
import skinemsya.vse.ru.api.internal.service.auth.AuthEventService;
import skinemsya.vse.ru.api.internal.service.auth.AuthGroupService;
import skinemsya.vse.ru.api.internal.service.auth.EventDeepLinkAccessService;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramChatService;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramInitDataValidator;
import skinemsya.vse.ru.api.internal.service.users.UserService;

/** Тесты основного сценария аутентификации. */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private TelegramInitDataValidator telegramInitDataValidator;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthGroupService authGroupService;

    @Mock
    private TelegramChatService telegramChatService;

    @Mock
    private AuthEventService authEventService;

    @Mock
    private EventDeepLinkAccessService eventDeepLinkAccessService;

    @InjectMocks
    private AuthServiceImpl authService;

    /** Проверяет создание сессии и выдачу токенов после проверки initData. */
    @Test
    void shouldAuthenticateTelegramUser() {
        var initData = createTelegramInitData();
        when(telegramInitDataValidator.validateWithChat("raw-init-data")).thenReturn(initData);
        when(userService.upsertFromTelegram(org.mockito.ArgumentMatchers.any())).thenReturn(createUser());
        when(jwtTokenService.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenService.accessTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(1L)).thenReturn(new IssuedRefreshToken("refresh-token", 1L));

        var result = authService.authenticate("raw-init-data");

        assertThat(result.getTokens().getAccessToken()).isEqualTo("access-token");
        assertThat(result.getTokens().getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getChatBootstrap()).isEmpty();
        verify(refreshTokenService).revokeAllActiveForUser(1L);
    }
}
