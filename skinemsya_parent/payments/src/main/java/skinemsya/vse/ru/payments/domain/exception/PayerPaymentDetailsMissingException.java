package skinemsya.vse.ru.payments.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.events.domain.exception.PaymentDetailsMissingException;

public class PayerPaymentDetailsMissingException extends PaymentDetailsMissingException {

    public PayerPaymentDetailsMissingException() {
        super();
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
