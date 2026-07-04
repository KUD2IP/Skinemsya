package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventNotInDistributionException extends EventsDomainException {

    public EventNotInDistributionException() {
        super("Event is not in distribution status");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
