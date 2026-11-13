package skinemsya.vse.ru.subscriptions.infrastructure.yookassa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.subscriptions.application.YooKassaClient;
import skinemsya.vse.ru.subscriptions.application.YooKassaPayment;
import skinemsya.vse.ru.subscriptions.infrastructure.config.SubscriptionProperties;

@Component
@ConditionalOnExpression(
        "!'${skinemsya.subscription.yookassa.shop-id:}'.isEmpty() and !'${skinemsya.subscription.yookassa.secret-key:}'.isEmpty()")
public class HttpYooKassaClient implements YooKassaClient {

    private static final Logger log = LoggerFactory.getLogger(HttpYooKassaClient.class);
    private static final String API = "https://api.yookassa.ru/v3/payments";

    private final SubscriptionProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String authorization;

    public HttpYooKassaClient(SubscriptionProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(20_000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        String token = properties.yookassa().shopId().trim() + ":"
                + properties.yookassa().secretKey().trim();
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public YooKassaPayment createCheckout(long userId, String phone, String idempotenceKey, boolean sbp) {
        ObjectNode body = baseBody(userId, phone, "checkout", "Распознавание чеков, 30 дней");
        String returnUrl = properties.yookassa().returnUrl();
        if (returnUrl == null || returnUrl.isBlank()) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Не задан адрес возврата после оплаты");
        }
        if (sbp) {
            body.putObject("payment_method_data").put("type", "sbp");
        }
        body.putObject("confirmation").put("type", "redirect").put("return_url", returnUrl.trim());
        return post(body, idempotenceKey);
    }

    @Override
    public YooKassaPayment createRenewal(long userId, String paymentMethodId, String phone, String idempotenceKey) {
        ObjectNode body = baseBody(userId, phone, "renewal", "Продление распознавания чеков, 30 дней");
        body.put("payment_method_id", paymentMethodId);
        return post(body, idempotenceKey);
    }

    @Override
    public YooKassaPayment getPayment(String paymentId) {
        try {
            String response = restClient
                    .get()
                    .uri(API + "/" + paymentId)
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (RestClientResponseException ex) {
            log.warn("YooKassa HTTP {}: {}", ex.getStatusCode().value(), errorDescription(ex));
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Платёж не подтвердился", ex);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Платёж не подтвердился", ex);
        }
    }

    private YooKassaPayment post(ObjectNode body, String idempotenceKey) {
        try {
            String response = restClient
                    .post()
                    .uri(API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authorization)
                    .header("Idempotence-Key", idempotenceKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parse(response);
        } catch (RestClientResponseException ex) {
            log.warn("YooKassa HTTP {}: {}", ex.getStatusCode().value(), errorDescription(ex));
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Не удалось создать оплату", ex);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Не удалось создать оплату", ex);
        }
    }

    private ObjectNode baseBody(long userId, String phone, String kind, String description) {
        String money = rubles(properties.priceKopecks());
        ObjectNode body = objectMapper.createObjectNode();
        body.put("capture", true);
        body.put("description", description);
        body.putObject("amount").put("value", money).put("currency", "RUB");
        body.putObject("metadata").put("user_id", Long.toString(userId)).put("kind", kind);
        if (properties.yookassa().receiptEnabled() && phone != null && !phone.isBlank()) {
            ObjectNode receipt = body.putObject("receipt");
            receipt.putObject("customer").put("phone", phone);
            ObjectNode item = receipt.putArray("items").addObject();
            item.put("description", description);
            item.put("quantity", "1.00");
            item.putObject("amount").put("value", money).put("currency", "RUB");
            item.put("vat_code", properties.yookassa().vatCode());
            item.put("payment_mode", "full_payment");
            item.put("payment_subject", "service");
        }
        return body;
    }

    private YooKassaPayment parse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
        String methodId = blankToNull(root.path("payment_method").path("id").asText(""));
        String confirmationUrl =
                blankToNull(root.path("confirmation").path("confirmation_url").asText(""));
        String confirmationToken =
                blankToNull(root.path("confirmation").path("confirmation_token").asText(""));
        String userRaw = root.path("metadata").path("user_id").asText("");
        Long userId = userRaw.isBlank() ? null : Long.valueOf(userRaw);
        return new YooKassaPayment(
                root.path("id").asText(""),
                root.path("status").asText(""),
                confirmationUrl,
                confirmationToken,
                methodId,
                userId,
                blankToNull(root.path("metadata").path("kind").asText("")));
    }

    static String rubles(long kopecks) {
        return String.format(Locale.US, "%.2f", kopecks / 100.0);
    }

    private String errorDescription(RestClientResponseException ex) {
        try {
            JsonNode root = objectMapper.readTree(ex.getResponseBodyAsString());
            String message = (root.path("code").asText("") + " "
                            + root.path("description").asText("") + " "
                            + root.path("parameter").asText(""))
                    .trim();
            if (message.length() > 300) {
                return message.substring(0, 300);
            }
            return message.isBlank() ? "no description" : message;
        } catch (JsonProcessingException ignored) {
            return "no description";
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
