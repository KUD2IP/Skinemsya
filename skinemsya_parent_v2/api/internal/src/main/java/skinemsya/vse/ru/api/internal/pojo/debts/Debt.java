package skinemsya.vse.ru.api.internal.pojo.debts;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.debts.DebtStatus;

/** Долг участника сбора перед плательщиком. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Debt {

    private long id;
    private long eventId;
    private long debtorId;
    private long creditorId;
    private long amountKopecks;
    private DebtStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
