package skinemsya.vse.ru.common.api;

import java.util.List;

/**
 * Standard API error response body.
 */
public record ApiErrorResponse(
        String code,
        String message,
        String correlationId,
        List<ApiErrorField> fields
) {

    public static ApiErrorResponse of(String code, String message, String correlationId) {
        return new ApiErrorResponse(code, message, correlationId, List.of());
    }

    public static ApiErrorResponse of(String code, String message, String correlationId, List<ApiErrorField> fields) {
        return new ApiErrorResponse(code, message, correlationId, fields == null ? List.of() : List.copyOf(fields));
    }
}
