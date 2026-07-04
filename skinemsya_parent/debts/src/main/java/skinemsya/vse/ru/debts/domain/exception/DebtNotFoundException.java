package skinemsya.vse.ru.debts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class DebtNotFoundException extends DebtsDomainException {

    public DebtNotFoundException() {
        super("Debt not found");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
