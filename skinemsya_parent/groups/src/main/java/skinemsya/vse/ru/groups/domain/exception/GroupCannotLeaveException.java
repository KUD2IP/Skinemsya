package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupCannotLeaveException extends GroupsDomainException {

    public GroupCannotLeaveException() {
        super("Владелец не может выйти из группы");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
