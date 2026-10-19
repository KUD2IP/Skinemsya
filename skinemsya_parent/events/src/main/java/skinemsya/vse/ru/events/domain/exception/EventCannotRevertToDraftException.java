package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventCannotRevertToDraftException extends EventsDomainException {

    public EventCannotRevertToDraftException() {
        super("Event can be returned to draft only before payments are confirmed");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
