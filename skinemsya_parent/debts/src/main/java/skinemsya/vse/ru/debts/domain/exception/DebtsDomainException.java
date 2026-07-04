package skinemsya.vse.ru.debts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.domain.TypedDomainException;

public abstract class DebtsDomainException extends RuntimeException implements TypedDomainException {

    protected DebtsDomainException(String message) {
        super(message);
    }

    @Override
    public abstract ErrorCode errorCode();
}
