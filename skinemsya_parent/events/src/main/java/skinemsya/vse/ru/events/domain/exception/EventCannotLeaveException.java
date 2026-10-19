package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventCannotLeaveException extends EventsDomainException {

    public EventCannotLeaveException() {
        super("Cannot leave this event");
    }

    public EventCannotLeaveException(String message) {
        super(message);
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
