package skinemsya.vse.ru.integrations.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlReceiptItem(
        String name,
        double quantity,
        @JsonProperty("unit_price") Double unitPrice,
        @JsonProperty("total_price") double totalPrice,
        Double confidence
) {
}
