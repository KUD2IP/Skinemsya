package skinemsya.vse.ru.events.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class EventNotDraftException extends EventsDomainException {

    public EventNotDraftException() {
        super("Event can only be modified in DRAFT status");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
