package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventExpectedParticipantCountException extends EventsDomainException {

    public EventExpectedParticipantCountException() {
        super("Expected participant count is invalid");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
