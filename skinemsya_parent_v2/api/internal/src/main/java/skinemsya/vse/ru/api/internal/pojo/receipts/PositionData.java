package skinemsya.vse.ru.api.internal.pojo.receipts;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Позиция чека, нужная для расчёта долга. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionData {

    private long id;
    private long totalPriceKopecks;
    private boolean shared;
    private BigDecimal quantity;
}
