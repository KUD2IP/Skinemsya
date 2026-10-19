package skinemsya.vse.ru.groups.domain.exception;

import skinemsya.vse.ru.common.domain.ErrorCode;

public class GroupMemberHasOpenDebtsException extends GroupsDomainException {

    public GroupMemberHasOpenDebtsException() {
        super("Нельзя выйти: вы уже отметили перевод, а плательщик его ещё не подтвердил");
    }

    @Override
    public ErrorCode errorCode() {
        return ErrorCode.DOMAIN_RULE_VIOLATION;
    }
}
