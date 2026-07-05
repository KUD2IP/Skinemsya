package skinemsya.vse.ru.app.testsupport;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import skinemsya.vse.ru.integrations.infrastructure.telegram.TelegramInitDataValidatorImpl;

public final class TelegramInitDataTestHelper {

    public static final String TEST_BOT_TOKEN = "test-bot-token-for-integration-tests-32chars";

    private TelegramInitDataTestHelper() {}

    public static String buildInitData(long telegramUserId, String firstName, Instant authDate) {
        return buildInitData(telegramUserId, firstName, authDate, null);
    }

    public static String buildInitData(long telegramUserId, String firstName, Instant authDate, String username) {
        return buildInitData(TEST_BOT_TOKEN, telegramUserId, firstName, authDate, username);
    }

    public static String buildInitData(String botToken, long telegramUserId, String firstName, Instant authDate) {
        return buildInitDataWithChat(botToken, telegramUserId, firstName, authDate, null, null, null, null);
    }

    public static String buildInitData(
            String botToken, long telegramUserId, String firstName, Instant authDate, String username) {
        return buildInitDataWithChat(botToken, telegramUserId, firstName, authDate, username, null, null, null);
    }

    public static String buildInitDataWithChat(
            long telegramUserId, String firstName, Instant authDate, long chatId, String chatTitle, String chatType) {
        return buildInitDataWithChat(
                TEST_BOT_TOKEN, telegramUserId, firstName, authDate, null, chatId, chatTitle, chatType);
    }

    public static String buildInitDataWithChat(
            String botToken,
            long telegramUserId,
            String firstName,
            Instant authDate,
            Long chatId,
            String chatTitle,
            String chatType) {
        return buildInitDataWithChat(botToken, telegramUserId, firstName, authDate, null, chatId, chatTitle, chatType);
    }

    public static String buildInitDataWithChat(
            String botToken,
            long telegramUserId,
            String firstName,
            Instant authDate,
            String username,
            Long chatId,
            String chatTitle,
            String chatType) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        var userJson = new StringBuilder("{\"id\":")
                .append(telegramUserId)
                .append(",\"first_name\":\"")
                .append(firstName)
                .append("\"");
        if (username != null && !username.isBlank()) {
            userJson.append(",\"username\":\"").append(username).append("\"");
        }
        userJson.append("}");
        params.put("user", userJson.toString());
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
            long telegramUserId, String firstName, Instant authDate, String startParam, String chatType) {
        return buildInitDataWithStartParam(TEST_BOT_TOKEN, telegramUserId, firstName, authDate, startParam, chatType);
    }

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
