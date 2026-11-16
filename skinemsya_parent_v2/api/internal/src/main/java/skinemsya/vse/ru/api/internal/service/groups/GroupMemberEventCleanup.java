package skinemsya.vse.ru.api.internal.service.groups;

/** Очистка связей пользователя с событиями при выходе из группы. */
public interface GroupMemberEventCleanup {

    /** Требует, чтобы пользователь не был плательщиком активных событий. */
    void assertUserIsNotPayerOfActiveEvents(long groupId, long userId);

    /** Требует отсутствия неподтверждённых переводов пользователя. */
    void assertUserHasNoUnconfirmedTransfer(long groupId, long userId);

    /** Удаляет пользователя из событий группы. */
    void removeUserFromGroupEvents(long groupId, long userId);
}
