package skinemsya.vse.ru.payments.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class PaymentInvalidStateException extends PaymentsDomainException {

    public PaymentInvalidStateException() {
        super("Payment is not in a valid state for this operation");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
