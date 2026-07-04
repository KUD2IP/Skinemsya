package skinemsya.vse.ru.events.domain;

import java.time.Instant;

public record Event(
        long id,
        long groupId,
        String name,
        String description,
        long payerId,
        long createdBy,
        EventStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
