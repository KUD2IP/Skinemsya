package skinemsya.vse.ru.integrations.infrastructure.ml;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import skinemsya.vse.ru.integrations.application.MlServiceClient;
import skinemsya.vse.ru.integrations.domain.MlReceiptItem;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;

import java.util.List;

@Component
@Primary
@ConditionalOnProperty(prefix = "skinemsya.ml-service", name = "url", havingValue = "", matchIfMissing = true)
public class StubMlServiceClient implements MlServiceClient {

    @Override
    public MlReceiptResponse recognize(byte[] imageBytes, String mimeType) {
        return new MlReceiptResponse(
                "Sample Cafe",
                null,
                850.0,
                "RUB",
                List.of(
                        new MlReceiptItem("Салат Цезарь", 1.0, 450.0, 450.0, 0.92),
                        new MlReceiptItem("Капучино", 2.0, 200.0, 400.0, 0.88)
                ),
                0.90,
                List.of()
        );
    }
}
