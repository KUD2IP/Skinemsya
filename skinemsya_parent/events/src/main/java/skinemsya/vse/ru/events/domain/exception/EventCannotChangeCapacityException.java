package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventCannotChangeCapacityException extends EventsDomainException {

    public EventCannotChangeCapacityException() {
        super("Expected participant count cannot be changed");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
