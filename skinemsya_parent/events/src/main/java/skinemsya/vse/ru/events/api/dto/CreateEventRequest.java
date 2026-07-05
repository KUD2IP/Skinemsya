package skinemsya.vse.ru.events.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @NotNull @Positive Long payerId) {}
