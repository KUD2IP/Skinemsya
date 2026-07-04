package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class InsufficientParticipantsException extends EventsDomainException {

    public InsufficientParticipantsException() {
        super("Event requires at least two participants for distribution");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
