package skinemsya.vse.ru.integrations.domain;

import java.util.List;

public record MlReceiptResponse(
        String merchant,
        String date,
        Double total,
        String currency,
        List<MlReceiptItem> items,
        Double confidence,
        List<String> errors) {}
