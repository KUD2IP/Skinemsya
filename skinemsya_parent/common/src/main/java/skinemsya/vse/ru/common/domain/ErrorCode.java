package skinemsya.vse.ru.common.domain;

/**
 * Stable machine-readable error codes for API, logs and tests.
 */
public enum ErrorCode {

    VALIDATION_ERROR("VALIDATION_ERROR", 400, ErrorSeverity.LOW, "Invalid request data"),
    AUTHENTICATION_ERROR("AUTHENTICATION_ERROR", 401, ErrorSeverity.LOW, "Authentication failed"),
    AUTHORIZATION_ERROR("AUTHORIZATION_ERROR", 403, ErrorSeverity.NONE, "Access denied"),
    NOT_FOUND("NOT_FOUND", 404, ErrorSeverity.LOW, "Resource not found"),
    DOMAIN_CONFLICT("DOMAIN_CONFLICT", 409, ErrorSeverity.LOW, "Domain state conflict"),
    DOMAIN_RULE_VIOLATION("DOMAIN_RULE_VIOLATION", 422, ErrorSeverity.LOW, "Business rule violation"),
    INTEGRATION_ERROR("INTEGRATION_ERROR", 502, ErrorSeverity.CRITICAL, "External system error"),
    INTERNAL_ERROR("INTERNAL_ERROR", 500, ErrorSeverity.CRITICAL, "Internal server error");

    private final String code;
    private final int httpStatus;
    private final ErrorSeverity severity;
    private final String description;

    ErrorCode(String code, int httpStatus, ErrorSeverity severity, String description) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.severity = severity;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public ErrorSeverity severity() {
        return severity;
    }

    public String description() {
        return description;
    }
}
