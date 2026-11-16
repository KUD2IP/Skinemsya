package skinemsya.vse.ru.api.internal.pojo.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Позиция распознанного чека. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlReceiptItem {

    private String name;
    private double quantity;
    @JsonProperty("unit_price")
    private Double unitPrice;
    @JsonProperty("total_price")
    private double totalPrice;
    private Double confidence;
}
