package skinemsya.vse.ru.payments.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class PaymentAccessDeniedException extends PaymentsDomainException {

    public PaymentAccessDeniedException() {
        super("Payment access denied");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
