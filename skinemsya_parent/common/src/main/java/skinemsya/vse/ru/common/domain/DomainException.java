package skinemsya.vse.ru.common.domain;

/**
 * Business rule violation or expected domain failure.
 */
public class DomainException extends RuntimeException implements TypedDomainException {

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
