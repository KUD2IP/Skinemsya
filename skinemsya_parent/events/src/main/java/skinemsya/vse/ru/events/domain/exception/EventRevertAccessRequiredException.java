package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventRevertAccessRequiredException extends EventsDomainException {

    public EventRevertAccessRequiredException() {
        super("Only payer or creator can return the event to draft");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
