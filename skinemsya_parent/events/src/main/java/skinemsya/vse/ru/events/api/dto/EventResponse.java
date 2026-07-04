package skinemsya.vse.ru.events.api.dto;

import skinemsya.vse.ru.events.domain.EventStatus;

import java.time.Instant;

public record EventResponse(
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
