package skinemsya.vse.ru.events.api.dto;

import java.time.Instant;
import skinemsya.vse.ru.events.domain.EventStatus;

public record EventResponse(
        long id,
        long groupId,
        String name,
        String description,
        long payerId,
        long createdBy,
        EventStatus status,
        Instant createdAt,
        Instant updatedAt) {}
