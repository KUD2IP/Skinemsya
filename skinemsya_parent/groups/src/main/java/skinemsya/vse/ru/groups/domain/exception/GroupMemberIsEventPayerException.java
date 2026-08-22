package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupMemberIsEventPayerException extends GroupsDomainException {

    public GroupMemberIsEventPayerException() {
        super("Cannot remove a member who is the payer of an active event");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
