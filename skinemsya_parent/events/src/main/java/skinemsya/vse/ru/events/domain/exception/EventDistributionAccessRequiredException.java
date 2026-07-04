package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventDistributionAccessRequiredException extends EventsDomainException {

    public EventDistributionAccessRequiredException() {
        super("Only payer or creator can send event to distribution");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
