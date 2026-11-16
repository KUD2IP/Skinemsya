package skinemsya.vse.ru.api.internal.pojo.events;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;

/** Внутреннее представление сбора. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    private long id;
    private long groupId;
    private String name;
    private String description;
    private long payerId;
    private long createdBy;
    private String inviteCode;
    private EventStatus status;
    private int expectedParticipantCount;
    private Instant createdAt;
    private Instant updatedAt;
}
