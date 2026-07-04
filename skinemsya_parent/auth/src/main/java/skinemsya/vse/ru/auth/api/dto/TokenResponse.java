package skinemsya.vse.ru.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        ChatBootstrapResponse chatBootstrap
) {
}
