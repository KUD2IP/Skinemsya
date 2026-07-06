package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventCloseAccessRequiredException extends EventsDomainException {

    public EventCloseAccessRequiredException() {
        super("Only payer can close the event");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
