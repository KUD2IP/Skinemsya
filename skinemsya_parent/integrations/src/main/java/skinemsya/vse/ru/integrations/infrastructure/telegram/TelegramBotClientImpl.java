package skinemsya.vse.ru.integrations.infrastructure.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.integrations.domain.TelegramSentMessage;
import skinemsya.vse.ru.integrations.infrastructure.config.TelegramIntegrationProperties;

import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TelegramBotClientImpl implements TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClientImpl.class);

    private final TelegramIntegrationProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private volatile String resolvedBotUsername;

    public TelegramBotClientImpl(
            TelegramIntegrationProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void initialize() {
        ensureBotUsernameResolved();
    }

    @Override
    public String getBotUsername() {
        return resolveBotUsername();
    }

    @Override
    public void sendHtmlMessage(long chatId, String text) {
        var payload = baseMessagePayload(chatId, text);
        payload.put("parse_mode", "HTML");
        callApi("sendMessage", payload);
    }

    @Override
    public TelegramSentMessage sendMessageWithOpenAppButton(
            long chatId,
            String text,
            String buttonText,
            String chatType,
            String startParam
    ) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("text", buttonText);
        if (isGroupChat(chatType)) {
            String deepLink = startParam != null && !startParam.isBlank()
                    ? startParam
                    : TelegramStartParam.forChat(chatId);
            button.put("url", buildMiniAppDeepLink(deepLink));
        } else {
            button.put("web_app", Map.of("url", requireMiniAppUrl()));
        }

        var payload = baseMessagePayload(chatId, text);
        payload.put("parse_mode", "HTML");
        payload.put("reply_markup", Map.of("inline_keyboard", List.of(List.of(button))));

        JsonNode result = callApi("sendMessage", payload);
        return new TelegramSentMessage(result.path("message_id").asLong());
    }

    @Override
    public void pinMessage(long chatId, long messageId) {
        callApi("pinChatMessage", Map.of(
                "chat_id", chatId,
                "message_id", messageId,
                "disable_notification", true
        ));
    }

    @Override
    public void sendMessage(long chatId, String text) {
        callApi("sendMessage", baseMessagePayload(chatId, text));
    }

    @Override
    public JsonNode getUpdates(long offset, int timeoutSeconds) {
        var botToken = requireBotToken();
        String allowedUpdates = URLEncoder.encode("[\"message\",\"my_chat_member\"]", StandardCharsets.UTF_8);
        String url = "https://api.telegram.org/bot" + botToken + "/getUpdates"
                + "?offset=" + offset
                + "&timeout=" + timeoutSeconds
                + "&allowed_updates=" + allowedUpdates;
        return callApiGet(url, "getUpdates");
    }

    @Override
    public Optional<String> getChatTitle(long chatId) {
        try {
            JsonNode chat = callApi("getChat", Map.of("chat_id", chatId));
            String title = chat.path("title").asText("");
            if (title.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(title.trim());
        } catch (RuntimeException ex) {
            log.warn("Failed to resolve Telegram chat title for chatId={}", chatId, ex);
            return Optional.empty();
        }
    }

    @Override
    public void deleteWebhook() {
        callApi("deleteWebhook", Map.of("drop_pending_updates", false));
        log.info("Telegram webhook removed; long polling can be used");
    }

    @Override
    public void setMyCommands() {
        List<Map<String, String>> commands = List.of(
                Map.of("command", "start", "description", "Начать работу с Skinemsya"),
                Map.of("command", "open", "description", "Открыть приложение"),
                Map.of("command", "help", "description", "Справка по командам")
        );
        callApi("setMyCommands", Map.of("commands", commands));
    }

    private String buildMiniAppDeepLink(String startParam) {
        String username = resolveBotUsername();
        String shortName = properties.webAppShortName();
        String base = shortName != null && !shortName.isBlank()
                ? "https://t.me/" + username + "/" + shortName.trim()
                : "https://t.me/" + username;
        if (startParam == null || startParam.isBlank()) {
            return base + "?startapp";
        }
        return base + "?startapp=" + startParam;
    }

    private String resolveBotUsername() {
        var configured = properties.botUsername();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        if (resolvedBotUsername != null && !resolvedBotUsername.isBlank()) {
            return resolvedBotUsername;
        }
        return ensureBotUsernameResolved();
    }

    private String ensureBotUsernameResolved() {
        var configured = properties.botUsername();
        if (configured != null && !configured.isBlank()) {
            resolvedBotUsername = configured.trim();
            return resolvedBotUsername;
        }
        if (resolvedBotUsername != null && !resolvedBotUsername.isBlank()) {
            return resolvedBotUsername;
        }

        var botToken = properties.botToken();
        if (botToken == null || botToken.isBlank()) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Telegram bot token is not configured");
        }

        String url = "https://api.telegram.org/bot" + botToken.trim() + "/getMe";
        JsonNode me = callApiGet(url, "getMe");
        resolvedBotUsername = me.path("username").asText("");
        if (resolvedBotUsername.isBlank()) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Telegram getMe returned empty username");
        }
        log.info("Resolved Telegram bot username via getMe: @{}", resolvedBotUsername);
        return resolvedBotUsername;
    }

    private String requireMiniAppUrl() {
        var url = properties.miniAppUrl();
        if (url == null || url.isBlank()) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Telegram Mini App URL is not configured");
        }
        return url.trim();
    }

    private static LinkedHashMap<String, Object> baseMessagePayload(long chatId, String text) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        return payload;
    }

    private static boolean isGroupChat(String chatType) {
        return "group".equals(chatType) || "supergroup".equals(chatType);
    }

    private JsonNode callApi(String method, Map<String, Object> payload) {
        var botToken = requireBotToken();
        String url = "https://api.telegram.org/bot" + botToken + "/" + method;
        try {
            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return extractResult(responseBody, method);
        } catch (RestClientResponseException ex) {
            throw new DomainException(
                    ErrorCode.INTEGRATION_ERROR,
                    "Telegram API HTTP error: " + ex.getStatusCode().value(),
                    ex
            );
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Telegram API call failed", ex);
        }
    }

    private JsonNode callApiGet(String url, String method) {
        try {
            String responseBody = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            return extractResult(responseBody, method);
        } catch (RestClientResponseException ex) {
            throw new DomainException(
                    ErrorCode.INTEGRATION_ERROR,
                    "Telegram API HTTP error: " + ex.getStatusCode().value(),
                    ex
            );
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            if ("getUpdates".equals(method) && isReadTimeout(ex)) {
                log.debug("Telegram long poll read timed out, continuing");
                return objectMapper.createArrayNode();
            }
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Telegram API call failed", ex);
        }
    }

    private static boolean isReadTimeout(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private JsonNode extractResult(String responseBody, String method) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!root.path("ok").asBoolean(false)) {
            String description = root.path("description").asText("Unknown Telegram API error");
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Telegram API error: " + description);
        }
        return root.path("result");
    }

    private String requireBotToken() {
        var botToken = properties.botToken();
        if (botToken == null || botToken.isBlank()) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Telegram bot token is not configured");
        }
        return botToken.trim();
    }
}
