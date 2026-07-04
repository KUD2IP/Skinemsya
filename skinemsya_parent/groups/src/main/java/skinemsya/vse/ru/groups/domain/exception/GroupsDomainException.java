package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.domain.TypedDomainException;

public abstract class GroupsDomainException extends RuntimeException implements TypedDomainException {

    protected GroupsDomainException(String message) {
        super(message);
    }

    protected GroupsDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public abstract ErrorCode errorCode();
}
