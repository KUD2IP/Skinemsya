package skinemsya.vse.ru.impl.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUser;
import static skinemsya.vse.ru.api.internal.pojo.factory.ServicePojoFactory.createUserProfile;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skinemsya.vse.ru.api.internal.security.AuthenticatedUser;
import skinemsya.vse.ru.api.internal.service.users.UserService;
import skinemsya.vse.ru.impl.mapper.rest.UserRestMapper;

/** Тесты REST API текущего пользователя. */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private UsernamePasswordAuthenticationToken authentication;

    /** Настраивает standalone MockMvc и аутентифицированного пользователя. */
    @BeforeEach
    void setUp() {
        UserRestMapper mapper = Mappers.getMapper(UserRestMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, mapper))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        authentication = new UsernamePasswordAuthenticationToken(new AuthenticatedUser(1L), null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** Очищает контекст безопасности после теста. */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Проверяет получение текущего пользователя. */
    @Test
    void shouldReturnCurrentUser() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(createUser()));
        when(userService.getProfile(1L)).thenReturn(createUserProfile());

        mockMvc.perform(get("/api/v1/users/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.telegramUserId").value(100_001L))
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.preferredBank").value("Bank"));
    }

    /** Проверяет обновление профиля текущего пользователя. */
    @Test
    void shouldUpdateCurrentUserProfile() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(createUser()));
        when(userService.updateProfile(1L, "+79990000000", "+79990000000", "Bank", "{\"enabled\":true}"))
                .thenReturn(createUserProfile());

        mockMvc.perform(put("/api/v1/users/me/profile")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "paymentDetails": "+79990000000",
                                  "phone": "+79990000000",
                                  "preferredBank": "Bank",
                                  "notificationSettings": "{\\"enabled\\":true}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentDetails").value("+79990000000"))
                .andExpect(jsonPath("$.notificationSettings").value("{\"enabled\":true}"));

        verify(userService).updateProfile(1L, "+79990000000", "+79990000000", "Bank", "{\"enabled\":true}");
    }
}
