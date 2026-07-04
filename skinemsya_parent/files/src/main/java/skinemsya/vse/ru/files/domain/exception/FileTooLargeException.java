package skinemsya.vse.ru.files.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class FileTooLargeException extends FilesDomainException {

    public FileTooLargeException(long maxSizeBytes) {
        super("File exceeds maximum size of " + formatMegabytes(maxSizeBytes));
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.VALIDATION_ERROR;
    }

    private static String formatMegabytes(long bytes) {
        long megabytes = Math.max(1, Math.round(bytes / (1024.0 * 1024.0)));
        return megabytes + " MB";
    }
}
