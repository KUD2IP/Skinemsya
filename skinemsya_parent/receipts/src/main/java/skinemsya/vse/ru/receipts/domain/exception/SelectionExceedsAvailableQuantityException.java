package skinemsya.vse.ru.receipts.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class SelectionExceedsAvailableQuantityException extends ReceiptsDomainException {

    public SelectionExceedsAvailableQuantityException() {
        super("Selected quantity exceeds remaining available quantity");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
