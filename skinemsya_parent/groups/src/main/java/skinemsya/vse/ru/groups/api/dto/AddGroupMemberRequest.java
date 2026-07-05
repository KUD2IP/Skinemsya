package skinemsya.vse.ru.groups.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddGroupMemberRequest(
        @NotBlank @Size(min = 5, max = 32) @Pattern(regexp = "^@?[a-zA-Z0-9_]{5,32}$") String telegramUsername) {}
