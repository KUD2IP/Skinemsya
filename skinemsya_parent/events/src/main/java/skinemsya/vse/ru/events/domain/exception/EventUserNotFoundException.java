package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventUserNotFoundException extends EventsDomainException {

    public EventUserNotFoundException() {
        super("User not found");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
