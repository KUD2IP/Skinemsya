package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventNotParticipantException extends EventsDomainException {

    public EventNotParticipantException() {
        super("User is not an event participant");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
