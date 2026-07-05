package skinemsya.vse.ru.integrations.infrastructure.ml;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.integrations.application.MlServiceClient;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;
import skinemsya.vse.ru.integrations.infrastructure.config.MlServiceProperties;

@Component
@ConditionalOnProperty(prefix = "skinemsya.ml-service", name = "url")
public class HttpMlServiceClient implements MlServiceClient {

    private final MlServiceProperties properties;
    private final RestClient restClient;

    public HttpMlServiceClient(MlServiceProperties properties) {
        this.properties = properties;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout((int) properties.timeout().toMillis());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public MlReceiptResponse recognize(byte[] imageBytes, String mimeType) {
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "receipt.jpg";
            }
        });

        String url = properties.url().trim();
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        url = url + "api/v1/recognize";

        try {
            return restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(MlReceiptResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                return retryOnce(url, body);
            }
            throw new DomainException(
                    ErrorCode.INTEGRATION_ERROR,
                    "ML service HTTP error: " + ex.getStatusCode().value(),
                    ex);
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "ML service call failed", ex);
        }
    }

    private MlReceiptResponse retryOnce(String url, MultiValueMap<String, Object> body) {
        try {
            return restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(MlReceiptResponse.class);
        } catch (Exception ex) {
            throw new DomainException(ErrorCode.INTEGRATION_ERROR, "ML service call failed after retry", ex);
        }
    }
}
