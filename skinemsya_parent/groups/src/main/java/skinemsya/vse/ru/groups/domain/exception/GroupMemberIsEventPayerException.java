package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupMemberIsEventPayerException extends GroupsDomainException {

    public GroupMemberIsEventPayerException() {
        super("Нельзя выйти из группы: вы плательщик в незакрытом сборе");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
