package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventNameTooLongException extends EventsDomainException {

    public EventNameTooLongException() {
        super("Event name is too long");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
