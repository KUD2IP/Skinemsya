package skinemsya.vse.ru.receipts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class SelectionCannotBeReopenedException extends ReceiptsDomainException {

    public SelectionCannotBeReopenedException() {
        super("Нельзя изменить выбор: перевод уже начат");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_CONFLICT;
    }
}
