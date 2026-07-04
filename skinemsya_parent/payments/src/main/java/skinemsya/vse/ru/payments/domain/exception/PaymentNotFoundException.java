package skinemsya.vse.ru.payments.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class PaymentNotFoundException extends PaymentsDomainException {

    public PaymentNotFoundException() {
        super("Payment not found");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
