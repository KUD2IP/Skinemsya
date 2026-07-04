package skinemsya.vse.ru.receipts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.common.domain.TypedDomainException;

public abstract class ReceiptsDomainException extends RuntimeException implements TypedDomainException {

    protected ReceiptsDomainException(String message) {
        super(message);
    }

    @Override
    public abstract ErrorCode errorCode();
}
