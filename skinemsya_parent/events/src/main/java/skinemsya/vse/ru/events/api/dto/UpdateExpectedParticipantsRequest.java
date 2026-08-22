package skinemsya.vse.ru.events.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateExpectedParticipantsRequest(
        @NotNull @Min(2) @Max(99) Integer expectedParticipantCount) {}
