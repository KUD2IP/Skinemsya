package skinemsya.vse.ru.integrations.infrastructure.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;
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
import skinemsya.vse.ru.integrations.application.MlServiceClient;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;
import skinemsya.vse.ru.integrations.infrastructure.config.ReceiptVisionProperties;

@Component
@ConditionalOnExpression("!'${skinemsya.receipt-vision.api-key:}'.isEmpty()")
public class OpenRouterVisionClient implements MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterVisionClient.class);

    private final ReceiptVisionProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenRouterVisionClient(ReceiptVisionProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout((int) properties.timeout().toMillis());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public MlReceiptResponse recognize(byte[] imageBytes, String mimeType) {
        ReceiptImageEncoder.EncodedImage image = ReceiptImageEncoder.toJpeg(imageBytes, properties.maxImageSide());
        String dataUrl =
                "data:" + image.mimeType() + ";base64," + Base64.getEncoder().encodeToString(image.bytes());
        log.debug("Receipt vision request, jpegBytes={}", image.bytes().length);

        MlReceiptResponse first = complete(dataUrl, OpenRouterReceiptParser.PROMPT);
        if (!OpenRouterReceiptParser.shouldRetry(first)) {
            return first;
        }
        MlReceiptResponse second =
                complete(dataUrl, OpenRouterReceiptParser.PROMPT + "\n\n" + OpenRouterReceiptParser.RETRY_NOTE);
        if (second.items() != null && !second.items().isEmpty()) {
            return second;
        }
        return first;
    }

    private MlReceiptResponse complete(String dataUrl, String prompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.model());
        body.put("temperature", 0);
        body.put("max_tokens", 4096);
        body.putObject("reasoning").put("enabled", false);
        body.putObject("response_format").put("type", "json_object");

        ArrayNode messages = body.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        content.addObject().put("type", "text").put("text", prompt);
        content.addObject().put("type", "image_url").putObject("image_url").put("url", dataUrl);

        String url = properties.baseUrl().trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        try {
            String responseBody = restClient
                    .post()
                    .uri(url + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey().trim())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(responseBody == null ? "{}" : responseBody);
            JsonNode messageNode = root.path("choices").path(0).path("message");
            return OpenRouterReceiptParser.parseContent(
                    OpenRouterReceiptParser.messageContent(messageNode), objectMapper);
        } catch (RestClientResponseException ex) {
            log.warn("OpenRouter HTTP {}", ex.getStatusCode().value());
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Сервис распознавания недоступен", ex);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("OpenRouter call failed");
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "Сервис распознавания недоступен", ex);
        }
    }
}
