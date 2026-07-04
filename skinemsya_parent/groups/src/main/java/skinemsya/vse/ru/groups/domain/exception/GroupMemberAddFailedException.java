package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupMemberAddFailedException extends GroupsDomainException {

    public GroupMemberAddFailedException() {
        super("Failed to add group member");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.INTERNAL_ERROR;
    }
}
