package skinemsya.vse.ru.groups.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(@NotBlank @Size(max = 255) String name) {
}
