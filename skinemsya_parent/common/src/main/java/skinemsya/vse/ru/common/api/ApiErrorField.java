package skinemsya.vse.ru.common.api;

/**
 * Validation field error in API error response.
 */
public record ApiErrorField(String field, String message) {}
