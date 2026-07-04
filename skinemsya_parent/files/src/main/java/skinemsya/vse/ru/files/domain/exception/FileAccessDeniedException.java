package skinemsya.vse.ru.files.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class FileAccessDeniedException extends FilesDomainException {

    public FileAccessDeniedException() {
        super("File access denied");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
