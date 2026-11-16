package skinemsya.vse.ru.impl.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramChatContext;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramIdentity;
import skinemsya.vse.ru.api.internal.pojo.telegram.TelegramInitData;
import skinemsya.vse.ru.api.internal.service.telegram.TelegramInitDataValidator;
import skinemsya.vse.ru.impl.support.properties.TelegramIntegrationProperties;
import skinemsya.vse.ru.impl.support.util.TelegramStartParam;

/** Проверка подписи Telegram initData по алгоритму HMAC-SHA256. */
@Component
public class TelegramInitDataValidatorImpl implements TelegramInitDataValidator {

    private static final String WEB_APP_DATA = "WebAppData";
    private static final String DEFAULT_GROUP_CHAT_TYPE = "supergroup";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TelegramIntegrationProperties properties;

    public TelegramInitDataValidatorImpl(TelegramIntegrationProperties properties) {
        this.properties = properties;
    }

    @Override
    public TelegramIdentity validate(String initData) {
        return validateWithChat(initData).getIdentity();
    }

    @Override
    public TelegramInitData validateWithChat(String initData) {
        var params = validateAndParseParams(initData);
        var identity = parseIdentity(params);
        var chat = resolveChat(params);
        var startParam = params.get("start_param");
        var eventInviteCode = TelegramStartParam.parseEventCode(startParam);
        var groupInviteCode = TelegramStartParam.parseGroupCode(startParam);
        return new TelegramInitData(identity, chat, eventInviteCode, groupInviteCode);
    }

    /** Извлекает контекст чата из параметров initData. */
    static Optional<TelegramChatContext> resolveChat(Map<String, String> params) {
        var chatFromJson = parseChat(params.get("chat"));
        if (chatFromJson.isPresent()) {
            return chatFromJson;
        }
        return TelegramStartParam.parseChatId(params.get("start_param"))
                .map(chatId -> new TelegramChatContext(chatId, "", resolveChatType(params.get("chat_type"))));
    }

    private static String resolveChatType(String chatType) {
        if (chatType == null || chatType.isBlank()) {
            return DEFAULT_GROUP_CHAT_TYPE;
        }
        return chatType.trim();
    }

    private Map<String, String> validateAndParseParams(String initData) {
        if (initData == null || initData.isBlank()) {
            throw new DomainException(
                    ErrorCode.AUTHENTICATION_ERROR,
                    "Telegram init data is missing. Open the Mini App through the bot, not in a regular browser");
        }
        if (properties.botToken() == null || properties.botToken().isBlank()) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, "Telegram bot token is not configured");
        }

        var normalizedInitData = normalizeInitData(initData);
        rejectJsonInitData(normalizedInitData);

        var params = parseQueryString(normalizedInitData);
        var receivedHash = params.remove("hash");
        if (receivedHash == null || receivedHash.isBlank()) {
            throw new DomainException(
                    ErrorCode.AUTHENTICATION_ERROR,
                    "Telegram init data hash is missing. Send the raw Telegram.WebApp.initData string unchanged, not initDataUnsafe");
        }

        var dataCheckString = buildDataCheckString(params);
        var calculatedHash = calculateHash(properties.botToken(), dataCheckString);
        if (!calculatedHash.equalsIgnoreCase(receivedHash)) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid Telegram init data signature");
        }

        validateAuthDate(params.get("auth_date"));
        return params;
    }

    private TelegramIdentity parseIdentity(Map<String, String> params) {
        var authDateRaw = params.get("auth_date");
        var authDate = Instant.ofEpochSecond(Long.parseLong(authDateRaw));

        var userJson = params.get("user");
        if (userJson == null || userJson.isBlank()) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Telegram user is missing");
        }

        try {
            JsonNode userNode = OBJECT_MAPPER.readTree(userJson);
            long telegramUserId = userNode.path("id").asLong();
            if (telegramUserId <= 0) {
                throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid Telegram user id");
            }
            String firstName = userNode.path("first_name").asText("");
            String lastName = userNode.path("last_name").asText("");
            String displayName = buildDisplayName(firstName, lastName);
            String username = userNode.path("username").asText("");
            String telegramUsername = username.isBlank() ? null : username.trim();
            String photoUrl = readPhotoUrl(userNode);
            return new TelegramIdentity(telegramUserId, displayName, authDate, telegramUsername, photoUrl);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid Telegram user payload", ex);
        }
    }

    private static String readPhotoUrl(JsonNode userNode) {
        String raw = userNode.path("photo_url").asText("");
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > 1024 || !trimmed.startsWith("https://")) {
            return null;
        }
        return trimmed;
    }

    private void validateAuthDate(String authDateRaw) {
        if (authDateRaw == null) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Telegram auth_date is missing");
        }

        long authDateEpoch;
        try {
            authDateEpoch = Long.parseLong(authDateRaw);
        } catch (NumberFormatException ex) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid Telegram auth_date");
        }

        var authDate = Instant.ofEpochSecond(authDateEpoch);
        var maxAge = Duration.ofSeconds(properties.authMaxAgeSeconds());
        if (authDate.isBefore(Instant.now().minus(maxAge))) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Telegram init data is expired");
        }
    }

    /** Разбирает JSON чата из initData. */
    static Optional<TelegramChatContext> parseChat(String chatJson) {
        if (chatJson == null || chatJson.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode chatNode = OBJECT_MAPPER.readTree(chatJson);
            long chatId = chatNode.path("id").asLong();
            if (chatId == 0) {
                return Optional.empty();
            }
            String title = chatNode.path("title").asText("");
            String chatType = chatNode.path("type").asText("");
            return Optional.of(new TelegramChatContext(chatId, title, chatType));
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.AUTHENTICATION_ERROR, "Invalid Telegram chat payload", ex);
        }
    }

    /** Нормализует initData: убирает префикс {@code ?} и раскодирует целиком закодированную строку. */
    static String normalizeInitData(String initData) {
        var normalized = initData.trim();
        if (normalized.startsWith("?")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.contains("&") && normalized.contains("%26")) {
            normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
        }
        return normalized;
    }

    private static void rejectJsonInitData(String initData) {
        if (initData.startsWith("{") || initData.startsWith("[")) {
            throw new DomainException(
                    ErrorCode.AUTHENTICATION_ERROR,
                    "Invalid init data format. Use Telegram.WebApp.initData query string, not initDataUnsafe JSON");
        }
    }

    /** Разбирает query string initData в отсортированную карту параметров. */
    static Map<String, String> parseQueryString(String initData) {
        Map<String, String> params = new TreeMap<>();
        for (String pair : initData.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    /** Формирует data-check-string для проверки подписи. */
    static String buildDataCheckString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        return keys.stream().map(key -> key + "=" + params.get(key)).collect(Collectors.joining("\n"));
    }

    /** Вычисляет HMAC-SHA256 подпись initData по токену бота. */
    public static String calculateHash(String botToken, String dataCheckString) {
        try {
            Mac hmacWebApp = Mac.getInstance("HmacSHA256");
            hmacWebApp.init(new SecretKeySpec(WEB_APP_DATA.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secretKey = hmacWebApp.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            Mac hmacData = Mac.getInstance("HmacSHA256");
            hmacData.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hash = hmacData.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate Telegram init data hash", ex);
        }
    }

    private static String buildDisplayName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
