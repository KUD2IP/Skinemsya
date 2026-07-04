package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupUserNotFoundException extends GroupsDomainException {

    public GroupUserNotFoundException() {
        super("User not found");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.NOT_FOUND;
    }
}
