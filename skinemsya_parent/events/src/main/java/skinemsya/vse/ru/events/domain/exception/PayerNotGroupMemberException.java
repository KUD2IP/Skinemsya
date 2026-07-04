package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class PayerNotGroupMemberException extends EventsDomainException {

    public PayerNotGroupMemberException() {
        super("Payer must be a group member");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
