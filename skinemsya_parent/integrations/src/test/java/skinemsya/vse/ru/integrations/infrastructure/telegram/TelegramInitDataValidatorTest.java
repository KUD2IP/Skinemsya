package skinemsya.vse.ru.integrations.infrastructure.telegram;

import org.junit.jupiter.api.Test;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.integrations.infrastructure.config.TelegramIntegrationProperties;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "test-bot-token-for-integration-tests-32chars";

    private final TelegramInitDataValidatorImpl validator = new TelegramInitDataValidatorImpl(
            new TelegramIntegrationProperties(BOT_TOKEN, 86_400, null, null, null, null, false)
    );

    @Test
    void shouldValidateCorrectInitData() {
        var initData = TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());

        var identity = validator.validate(initData);

        assertThat(identity.telegramUserId()).isEqualTo(100_001L);
        assertThat(identity.displayName()).isEqualTo("Alice");
    }

    @Test
    void shouldRejectInvalidSignature() {
        var initData = TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now()) + "tampered";

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void shouldRejectExpiredAuthDate() {
        var initData = TelegramInitDataTestHelper.buildInitData(
                BOT_TOKEN,
                100_001L,
                "Alice",
                Instant.now().minusSeconds(86_401)
        );

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldValidateFullyUrlEncodedInitData() {
        var initData = TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());
        var fullyEncoded = URLEncoder.encode(initData, StandardCharsets.UTF_8);

        var identity = validator.validate(fullyEncoded);

        assertThat(identity.telegramUserId()).isEqualTo(100_001L);
    }

    @Test
    void shouldValidateInitDataWithLeadingQuestionMark() {
        var initData = "?" + TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());

        var identity = validator.validate(initData);

        assertThat(identity.telegramUserId()).isEqualTo(100_001L);
    }

    @Test
    void shouldRejectJsonInitDataUnsafePayload() {
        assertThatThrownBy(() -> validator.validate("{\"id\":100001,\"first_name\":\"Alice\"}"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("initDataUnsafe");
    }

    @Test
    void shouldRejectPlainTextWithoutHash() {
        assertThatThrownBy(() -> validator.validate("test"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("hash is missing");
    }

    @Test
    void shouldExtractChatFromInitData() {
        var initData = TelegramInitDataTestHelper.buildInitDataWithChat(
                BOT_TOKEN,
                100_001L,
                "Alice",
                Instant.now(),
                -1_002L,
                "Trip chat",
                "group"
        );

        var result = validator.validateWithChat(initData);

        assertThat(result.identity().telegramUserId()).isEqualTo(100_001L);
        assertThat(result.chat()).isPresent();
        assertThat(result.chat().get().chatId()).isEqualTo(-1_002L);
        assertThat(result.chat().get().title()).isEqualTo("Trip chat");
    }

    @Test
    void shouldReturnEmptyChatWhenNotPresent() {
        var initData = TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());

        var result = validator.validateWithChat(initData);

        assertThat(result.chat()).isEmpty();
    }

    @Test
    void shouldExtractChatFromStartParamWhenChatJsonMissing() {
        var initData = TelegramInitDataTestHelper.buildInitDataWithStartParam(
                BOT_TOKEN,
                100_001L,
                "Alice",
                Instant.now(),
                "chat_-100777",
                "supergroup"
        );

        var result = validator.validateWithChat(initData);

        assertThat(result.chat()).isPresent();
        assertThat(result.chat().get().chatId()).isEqualTo(-100_777L);
        assertThat(result.chat().get().chatType()).isEqualTo("supergroup");
    }
}

class TelegramInitDataTestHelper {

    private TelegramInitDataTestHelper() {
    }

    public static String buildInitData(String botToken, long telegramUserId, String firstName, Instant authDate) {
        return buildInitDataWithChat(botToken, telegramUserId, firstName, authDate, null, null, null);
    }

    public static String buildInitDataWithChat(
            String botToken,
            long telegramUserId,
            String firstName,
            Instant authDate,
            Long chatId,
            String chatTitle,
            String chatType
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        params.put("user", "{\"id\":" + telegramUserId + ",\"first_name\":\"" + firstName + "\"}");
        if (chatId != null) {
            params.put("chat", "{\"id\":" + chatId + ",\"title\":\"" + chatTitle + "\",\"type\":\"" + chatType + "\"}");
        }

        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));

        String hash = TelegramInitDataValidatorImpl.calculateHash(botToken, dataCheckString);

        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"))
                + "&hash=" + hash;
    }

    public static String buildInitDataWithStartParam(
            String botToken,
            long telegramUserId,
            String firstName,
            Instant authDate,
            String startParam,
            String chatType
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        params.put("user", "{\"id\":" + telegramUserId + ",\"first_name\":\"" + firstName + "\"}");
        params.put("start_param", startParam);
        if (chatType != null && !chatType.isBlank()) {
            params.put("chat_type", chatType);
        }

        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));

        String hash = TelegramInitDataValidatorImpl.calculateHash(botToken, dataCheckString);

        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"))
                + "&hash=" + hash;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
