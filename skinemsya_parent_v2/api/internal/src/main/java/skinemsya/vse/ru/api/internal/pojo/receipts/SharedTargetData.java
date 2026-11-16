package skinemsya.vse.ru.api.internal.pojo.receipts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Участник, на которого делится общая позиция. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedTargetData {

    private long positionId;
    private long userId;
}
