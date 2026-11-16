package skinemsya.vse.ru.api.internal.pojo.events;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Участник сбора. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipant {

    private long id;
    private long eventId;
    private long userId;
    private Instant selectionCompletedAt;
}
