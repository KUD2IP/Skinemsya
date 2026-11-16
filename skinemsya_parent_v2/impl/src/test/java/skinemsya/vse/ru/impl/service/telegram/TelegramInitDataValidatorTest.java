package skinemsya.vse.ru.impl.service.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.impl.support.properties.TelegramIntegrationProperties;

/** Тесты проверки подписи и разбора Telegram initData. */
class TelegramInitDataValidatorTest {

    private static final String BOT_TOKEN = "test-bot-token-for-integration-tests-32chars";

    private final TelegramInitDataValidatorImpl validator = new TelegramInitDataValidatorImpl(
            new TelegramIntegrationProperties(BOT_TOKEN, 86_400, null, null, null, null, false));

    @Test
    void shouldValidateCorrectInitData() {
        var initData = TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());

        var identity = validator.validate(initData);

        assertThat(identity.getTelegramUserId()).isEqualTo(100_001L);
        assertThat(identity.getDisplayName()).isEqualTo("Alice");
        assertThat(identity.getPhotoUrl()).isNull();
    }

    @Test
    void shouldKeepHttpsPhotoUrl() {
        var photoUrl = "https://t.me/i/userpic/320/alice.jpg";
        var initData = TelegramInitDataTestHelper.buildInitDataWithPhoto(
                BOT_TOKEN, 100_001L, "Alice", Instant.now(), photoUrl);

        var identity = validator.validate(initData);

        assertThat(identity.getPhotoUrl()).isEqualTo(photoUrl);
    }

    @Test
    void shouldDropPhotoUrlThatIsNotHttps() {
        var initData = TelegramInitDataTestHelper.buildInitDataWithPhoto(
                BOT_TOKEN, 100_001L, "Alice", Instant.now(), "http://evil.example/a.jpg");

        var identity = validator.validate(initData);

        assertThat(identity.getPhotoUrl()).isNull();
    }

    @Test
    void shouldRejectInvalidSignature() {
        var initData =
                TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now()) + "tampered";

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void shouldRejectExpiredAuthDate() {
        var initData = TelegramInitDataTestHelper.buildInitData(
                BOT_TOKEN, 100_001L, "Alice", Instant.now().minusSeconds(86_401));

        assertThatThrownBy(() -> validator.validate(initData))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldValidateFullyUrlEncodedInitData() {
        var initData = TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());
        var fullyEncoded = URLEncoder.encode(initData, StandardCharsets.UTF_8);

        var identity = validator.validate(fullyEncoded);

        assertThat(identity.getTelegramUserId()).isEqualTo(100_001L);
    }

    @Test
    void shouldValidateInitDataWithLeadingQuestionMark() {
        var initData = "?" + TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());

        var identity = validator.validate(initData);

        assertThat(identity.getTelegramUserId()).isEqualTo(100_001L);
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
                BOT_TOKEN, 100_001L, "Alice", Instant.now(), -1_002L, "Trip chat", "group");

        var result = validator.validateWithChat(initData);

        assertThat(result.getIdentity().getTelegramUserId()).isEqualTo(100_001L);
        assertThat(result.getChat()).isPresent();
        assertThat(result.getChat().get().getChatId()).isEqualTo(-1_002L);
        assertThat(result.getChat().get().getTitle()).isEqualTo("Trip chat");
    }

    @Test
    void shouldReturnEmptyChatWhenNotPresent() {
        var initData = TelegramInitDataTestHelper.buildInitData(BOT_TOKEN, 100_001L, "Alice", Instant.now());

        var result = validator.validateWithChat(initData);

        assertThat(result.getChat()).isEmpty();
    }

    @Test
    void shouldExtractChatFromStartParamWhenChatJsonMissing() {
        var initData = TelegramInitDataTestHelper.buildInitDataWithStartParam(
                BOT_TOKEN, 100_001L, "Alice", Instant.now(), "chat_-100777", "supergroup");

        var result = validator.validateWithChat(initData);

        assertThat(result.getChat()).isPresent();
        assertThat(result.getChat().get().getChatId()).isEqualTo(-100_777L);
        assertThat(result.getChat().get().getChatType()).isEqualTo("supergroup");
    }
}

/** Построитель подписанных Telegram initData для тестов. */
class TelegramInitDataTestHelper {

    private TelegramInitDataTestHelper() {}

    /** Формирует подписанные initData без чата. */
    public static String buildInitData(String botToken, long telegramUserId, String firstName, Instant authDate) {
        return buildInitDataWithChat(botToken, telegramUserId, firstName, authDate, null, null, null);
    }

    /** Формирует подписанные initData с фото профиля. */
    public static String buildInitDataWithPhoto(
            String botToken, long telegramUserId, String firstName, Instant authDate, String photoUrl) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        params.put(
                "user",
                "{\"id\":"
                        + telegramUserId
                        + ",\"first_name\":\""
                        + firstName
                        + "\",\"photo_url\":\""
                        + photoUrl
                        + "\"}");
        return sign(botToken, params);
    }

    /** Формирует подписанные initData с контекстом чата. */
    public static String buildInitDataWithChat(
            String botToken,
            long telegramUserId,
            String firstName,
            Instant authDate,
            Long chatId,
            String chatTitle,
            String chatType) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        params.put("user", "{\"id\":" + telegramUserId + ",\"first_name\":\"" + firstName + "\"}");
        if (chatId != null) {
            params.put("chat", "{\"id\":" + chatId + ",\"title\":\"" + chatTitle + "\",\"type\":\"" + chatType + "\"}");
        }
        return sign(botToken, params);
    }

    /** Формирует подписанные initData со start parameter. */
    public static String buildInitDataWithStartParam(
            String botToken,
            long telegramUserId,
            String firstName,
            Instant authDate,
            String startParam,
            String chatType) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        params.put("user", "{\"id\":" + telegramUserId + ",\"first_name\":\"" + firstName + "\"}");
        params.put("start_param", startParam);
        if (chatType != null && !chatType.isBlank()) {
            params.put("chat_type", chatType);
        }
        return sign(botToken, params);
    }

    private static String sign(String botToken, Map<String, String> params) {
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
