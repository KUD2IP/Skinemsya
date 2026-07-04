package skinemsya.vse.ru.common.domain;

/**
 * Correlation identifier for request tracing across logs and error responses.
 */
public record CorrelationId(String value) {

    public static final String HEADER_NAME = "X-Correlation-Id";

    public CorrelationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Correlation id must not be blank");
        }
    }

    public static CorrelationId of(String value) {
        return new CorrelationId(value);
    }
}
