package skinemsya.vse.ru.api.internal.pojo.debts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Сводка долгов пользователя. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebtSummary {

    private long totalOwedKopecks;
    private long totalToReceiveKopecks;
    private long unpaidCount;
    private long pendingCount;
}
