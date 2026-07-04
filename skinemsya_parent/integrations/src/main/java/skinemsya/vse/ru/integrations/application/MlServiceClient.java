package skinemsya.vse.ru.integrations.application;

import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;

public interface MlServiceClient {

    MlReceiptResponse recognize(byte[] imageBytes, String mimeType);
}
