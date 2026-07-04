package skinemsya.vse.ru.payments.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.domain.TypedDomainException;

public abstract class PaymentsDomainException extends RuntimeException implements TypedDomainException {

    protected PaymentsDomainException(String message) {
        super(message);
    }

    @Override
    public abstract ErrorCode errorCode();
}
