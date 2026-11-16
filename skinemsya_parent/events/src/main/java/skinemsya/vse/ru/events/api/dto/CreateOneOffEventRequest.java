package skinemsya.vse.ru.events.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOneOffEventRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @NotNull @Min(2) @Max(99) Integer expectedParticipantCount) {}
