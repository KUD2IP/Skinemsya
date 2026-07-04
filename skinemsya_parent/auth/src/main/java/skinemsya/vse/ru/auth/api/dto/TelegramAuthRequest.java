package skinemsya.vse.ru.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record TelegramAuthRequest(
        @NotBlank @JsonAlias("init_data") String initData
) {
}
