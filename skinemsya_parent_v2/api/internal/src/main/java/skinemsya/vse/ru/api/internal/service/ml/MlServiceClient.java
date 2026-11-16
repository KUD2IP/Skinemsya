package skinemsya.vse.ru.api.internal.service.ml;

import skinemsya.vse.ru.api.internal.pojo.ml.MlReceiptResponse;

/** Клиент сервиса распознавания чеков. */
public interface MlServiceClient {

    /** Распознаёт чек по изображению. */
    MlReceiptResponse recognize(byte[] imageBytes, String mimeType);
}
