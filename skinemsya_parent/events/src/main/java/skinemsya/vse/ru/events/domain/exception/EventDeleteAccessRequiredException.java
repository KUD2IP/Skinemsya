package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventDeleteAccessRequiredException extends EventsDomainException {

    public EventDeleteAccessRequiredException() {
        super("Event delete access required");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
