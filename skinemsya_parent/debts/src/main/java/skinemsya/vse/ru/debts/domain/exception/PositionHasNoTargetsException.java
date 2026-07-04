package skinemsya.vse.ru.debts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class PositionHasNoTargetsException extends DebtsDomainException {

    public PositionHasNoTargetsException() {
        super("Position has no selected or shared targets");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
