package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupNameRequiredException extends GroupsDomainException {

    public GroupNameRequiredException() {
        super("Group name is required");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.VALIDATION_ERROR;
    }
}
