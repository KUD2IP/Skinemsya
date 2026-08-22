package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupCannotRemoveOwnerException extends GroupsDomainException {

    public GroupCannotRemoveOwnerException() {
        super("Cannot remove the group owner");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
