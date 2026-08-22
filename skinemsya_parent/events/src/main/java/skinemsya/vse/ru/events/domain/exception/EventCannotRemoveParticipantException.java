package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventCannotRemoveParticipantException extends EventsDomainException {

    public EventCannotRemoveParticipantException() {
        super("Cannot remove this participant from the event");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
