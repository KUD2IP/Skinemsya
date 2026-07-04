package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class UserNotFoundByTelegramUsernameException extends GroupsDomainException {

    public UserNotFoundByTelegramUsernameException() {
        super("User not found by Telegram username. Ask them to open the Mini App first.");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
