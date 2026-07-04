package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.domain.TypedDomainException;

public abstract class EventsDomainException extends RuntimeException implements TypedDomainException {

    protected EventsDomainException(String message) {
        super(message);
    }

    @Override
    public abstract ErrorCode errorCode();
}
