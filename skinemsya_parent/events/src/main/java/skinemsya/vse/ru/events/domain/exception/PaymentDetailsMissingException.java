package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class PaymentDetailsMissingException extends EventsDomainException {

    public PaymentDetailsMissingException() {
        super("Payer payment details are required before distribution");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
