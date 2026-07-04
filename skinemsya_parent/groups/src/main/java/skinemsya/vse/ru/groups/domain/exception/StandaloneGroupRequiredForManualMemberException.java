package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class StandaloneGroupRequiredForManualMemberException extends GroupsDomainException {

    public StandaloneGroupRequiredForManualMemberException() {
        super("Members can only be added manually to standalone groups");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
