package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventNameRequiredException extends EventsDomainException {

    public EventNameRequiredException() {
        super("Event name is required");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
