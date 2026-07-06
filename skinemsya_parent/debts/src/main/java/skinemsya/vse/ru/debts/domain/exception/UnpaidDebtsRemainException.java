package skinemsya.vse.ru.debts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class UnpaidDebtsRemainException extends DebtsDomainException {

    public UnpaidDebtsRemainException() {
        super("All debts must be paid before closing the event");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
