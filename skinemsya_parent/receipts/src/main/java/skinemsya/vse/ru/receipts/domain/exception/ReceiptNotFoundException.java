package skinemsya.vse.ru.receipts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class ReceiptNotFoundException extends ReceiptsDomainException {

    public ReceiptNotFoundException() {
        super("Receipt not found");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
