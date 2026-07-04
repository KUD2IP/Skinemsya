package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupHasBlockingEventsException extends GroupsDomainException {

    public GroupHasBlockingEventsException() {
        super("Group cannot be deleted while it has non-draft events");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
