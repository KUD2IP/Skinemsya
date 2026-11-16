package skinemsya.vse.ru.api.internal.pojo.ml;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Результат распознавания чека. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlReceiptResponse {

    private String merchant;
    private String date;
    private Double total;
    private String currency;
    private List<MlReceiptItem> items;
    private Double confidence;
    private List<String> errors;
}
