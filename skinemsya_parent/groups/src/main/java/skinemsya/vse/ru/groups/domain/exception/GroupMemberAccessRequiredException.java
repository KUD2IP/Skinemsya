package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupMemberAccessRequiredException extends GroupsDomainException {

    public GroupMemberAccessRequiredException() {
        super("User is not a group member");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
