package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class UserNotFoundByTelegramIdException extends GroupsDomainException {

    public UserNotFoundByTelegramIdException() {
        super("User not found by Telegram id");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
