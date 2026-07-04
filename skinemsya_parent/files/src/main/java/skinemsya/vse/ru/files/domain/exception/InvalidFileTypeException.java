package skinemsya.vse.ru.files.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.files.application.FileUploadPurpose;

public class InvalidFileTypeException extends FilesDomainException {

    public InvalidFileTypeException() {
        super("Only image files are allowed");
    }

    public InvalidFileTypeException(FileUploadPurpose purpose) {
        super(switch (purpose) {
            case PAYMENT_PROOF -> "Only image or PDF files are allowed for payment proof";
            case RECEIPT -> "Only image files are allowed for receipt upload";
        });
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
