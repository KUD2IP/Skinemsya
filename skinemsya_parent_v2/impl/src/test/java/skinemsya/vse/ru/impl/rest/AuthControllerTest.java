package skinemsya.vse.ru.impl.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createAuthResult;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createTokenPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skinemsya.vse.ru.api.internal.service.auth.AuthService;
import skinemsya.vse.ru.impl.mapper.rest.AuthRestMapper;

/** Тесты REST API аутентификации. */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    /** Настраивает standalone MockMvc с реальным MapStruct-маппером. */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, Mappers.getMapper(AuthRestMapper.class)))
                .build();
    }

    /** Проверяет аутентификацию и alias init_data из Telegram. */
    @Test
    void shouldAuthenticateUsingTelegramInitDataAlias() throws Exception {
        when(authService.authenticate("telegram-init-data")).thenReturn(createAuthResult());

        mockMvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"init_data\":\"telegram-init-data\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.chatBootstrap.groupId").value(10));
    }

    /** Проверяет обновление пары токенов. */
    @Test
    void shouldRefreshTokens() throws Exception {
        when(authService.refresh("refresh-token")).thenReturn(createTokenPair());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }
}
