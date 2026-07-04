package skinemsya.vse.ru.receipts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class PositionNotFoundException extends ReceiptsDomainException {

    public PositionNotFoundException() {
        super("Position not found");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
