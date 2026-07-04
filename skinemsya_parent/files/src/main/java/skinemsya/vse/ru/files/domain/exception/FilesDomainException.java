package skinemsya.vse.ru.files.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.domain.TypedDomainException;

public abstract class FilesDomainException extends RuntimeException implements TypedDomainException {

    protected FilesDomainException(String message) {
        super(message);
    }

    @Override
    public abstract ErrorCode errorCode();
}
