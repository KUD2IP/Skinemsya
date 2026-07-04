package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventNotCalculatedException extends EventsDomainException {

    public EventNotCalculatedException() {
        super("Event debts are not calculated yet");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
