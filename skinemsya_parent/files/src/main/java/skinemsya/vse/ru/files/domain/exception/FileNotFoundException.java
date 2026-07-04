package skinemsya.vse.ru.files.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class FileNotFoundException extends FilesDomainException {

    public FileNotFoundException() {
        super("File not found");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
