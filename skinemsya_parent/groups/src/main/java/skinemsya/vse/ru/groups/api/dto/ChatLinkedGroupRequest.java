package skinemsya.vse.ru.groups.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatLinkedGroupRequest(@NotBlank String initData) {}
