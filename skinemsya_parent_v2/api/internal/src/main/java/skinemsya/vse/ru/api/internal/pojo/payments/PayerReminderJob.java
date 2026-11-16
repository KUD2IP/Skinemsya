package skinemsya.vse.ru.api.internal.pojo.payments;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Отложенное напоминание плательщику. Сейчас не отправляется: уведомления идут в чат группы. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayerReminderJob {

    private long id;
    private long paymentId;
    private Instant scheduledAt;
    private Instant sentAt;
}
