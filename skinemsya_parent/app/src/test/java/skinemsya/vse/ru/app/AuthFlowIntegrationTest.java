package skinemsya.vse.ru.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import skinemsya.vse.ru.app.testsupport.TelegramInitDataTestHelper;
import skinemsya.vse.ru.auth.infrastructure.persistence.RefreshTokenRepository;
import skinemsya.vse.ru.users.infrastructure.persistence.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("skinemsya")
            .withUsername("skinemsya")
            .withPassword("skinemsya");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldAuthenticateWithValidTelegramInitData() throws Exception {
        var initData = TelegramInitDataTestHelper.buildInitData(100_001L, "Alice", Instant.now());

        mockMvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void shouldRejectInvalidTelegramSignature() throws Exception {
        var initData = TelegramInitDataTestHelper.buildInitData(100_001L, "Alice", Instant.now()) + "tampered";

        mockMvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
    }

    @Test
    void shouldRefreshValidTokenAndRevokeOldOne() throws Exception {
        var tokens = authenticate();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        assertThat(refreshTokenRepository.findByTokenHash(
                        skinemsya.vse.ru.auth.application.RefreshTokenService.hash(tokens.refreshToken())))
                .get()
                .satisfies(entity -> assertThat(entity.isRevoked()).isTrue());
    }

    @Test
    void shouldRejectExpiredOrRevokedRefreshToken() throws Exception {
        var tokens = authenticate();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
    }

    @Test
    void shouldRejectUsersMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserWithAccessToken() throws Exception {
        var tokens = authenticate();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramUserId").value(100_001))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void shouldJoinChatLinkedGroupOnAuthWithoutNavigationBootstrap() throws Exception {
        var initData = TelegramInitDataTestHelper.buildInitDataWithChat(
                100_010L, "ChatUser", Instant.now(), -100_777L, "Office lunch", "supergroup");

        var authResponse = mockMvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.chatBootstrap.groupId").isNumber())
                .andExpect(jsonPath("$.chatBootstrap.groupName").value("Office lunch"))
                .andExpect(jsonPath("$.chatBootstrap.groupType").value("CHAT_LINKED"))
                .andExpect(jsonPath("$.chatBootstrap.suggestedAction").value("OPEN_APP"))
                .andReturn();

        var accessToken = readJsonField(authResponse.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/v1/groups").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Office lunch"))
                .andExpect(jsonPath("$.items[0].type").value("CHAT_LINKED"));
    }

    @Test
    void shouldJoinChatLinkedGroupFromStartParamOnAuth() throws Exception {
        var initData = TelegramInitDataTestHelper.buildInitDataWithStartParam(
                100_011L, "DeepLinkUser", Instant.now(), "chat_-100778", "supergroup");

        var authResponse = mockMvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatBootstrap.groupId").isNumber())
                .andExpect(jsonPath("$.chatBootstrap.groupType").value("CHAT_LINKED"))
                .andExpect(jsonPath("$.chatBootstrap.suggestedAction").value("OPEN_APP"))
                .andReturn();

        var accessToken = readJsonField(authResponse.getResponse().getContentAsString(), "accessToken");

        mockMvc.perform(get("/api/v1/groups").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("CHAT_LINKED"))
                .andExpect(jsonPath("$.items[0].telegramChatId").value(-100_778));
    }

    @Test
    void shouldRevokePreviousRefreshTokensOnReLogin() throws Exception {
        var initData = TelegramInitDataTestHelper.buildInitData(100_030L, "RepeatUser", Instant.now());
        var firstTokens = authenticate(initData);

        authenticate(initData);

        var userId = userRepository.findByTelegramUserId(100_030L).orElseThrow().getId();
        var activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtAfter(userId, Instant.now());
        assertThat(activeTokens).hasSize(1);
        assertThat(refreshTokenRepository.findByTokenHash(
                        skinemsya.vse.ru.auth.application.RefreshTokenService.hash(firstTokens.refreshToken())))
                .get()
                .satisfies(entity -> assertThat(entity.isRevoked()).isTrue());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstTokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_ERROR"));
    }

    private TokenPair authenticate() throws Exception {
        return authenticate(TelegramInitDataTestHelper.buildInitData(100_001L, "Alice", Instant.now()));
    }

    private TokenPair authenticate(String initData) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        var response = result.getResponse().getContentAsString();
        var accessToken = readJsonField(response, "accessToken");
        var refreshToken = readJsonField(response, "refreshToken");
        return new TokenPair(accessToken, refreshToken);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readJsonField(String json, String field) {
        var marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Field not found: " + field);
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private record TokenPair(String accessToken, String refreshToken) {}
}
