package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupOwnerAccessRequiredException extends GroupsDomainException {

    public GroupOwnerAccessRequiredException() {
        super("Group owner access required");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.AUTHORIZATION_ERROR;
    }
}
