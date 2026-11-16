package skinemsya.vse.ru.api.internal.pojo.receipts;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Выбор количества позиции участником. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectionData {

    private long positionId;
    private long userId;
    private BigDecimal selectedQuantity;
}
