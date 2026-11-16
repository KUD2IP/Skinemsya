package skinemsya.vse.ru.api.internal.ex;

import skinemsya.vse.ru.api.internal.enums.ErrorCode;

/** Ожидаемое нарушение бизнес-правила со стабильным кодом ошибки. */
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

    @Override
    public ErrorCode errorCode() {
        return errorCode;
    }
}
