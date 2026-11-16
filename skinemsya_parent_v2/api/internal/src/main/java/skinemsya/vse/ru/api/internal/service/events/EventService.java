package skinemsya.vse.ru.api.internal.service.events;

import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.events.Event;

/** Операции со сборами. */
public interface EventService {

    /** Создаёт черновик сбора и добавляет создателя и плательщика. */
    Event create(
            long groupId, String name, String description, long payerId, long creatorId, int expectedParticipantCount);

    /** Обновляет черновик сбора. */
    Event update(
            long eventId,
            long requesterId,
            String name,
            String description,
            long payerId,
            int expectedParticipantCount);

    /** Добавляет участника группы в сбор. */
    Event join(long eventId, long userId);

    /** Выводит участника из сбора. */
    Event leave(long eventId, long userId);

    /** Возвращает сбор в черновик до подтверждённых переводов. */
    Event revertToDraft(long eventId, long requesterId);

    /** Удаляет участника из сбора от имени владельца группы. */
    Event removeParticipant(long eventId, long requesterId, long targetUserId);

    /** Меняет ожидаемое число участников. */
    Event updateExpectedParticipantCount(long eventId, long requesterId, int expectedParticipantCount);

    /** Находит активный сбор по идентификатору. */
    Optional<Event> findById(long eventId);

    /** Возвращает код приглашения сбора. */
    String inviteCode(long eventId);

    /** Находит идентификатор сбора по коду приглашения. */
    Optional<Long> findIdByInviteCode(String inviteCode);

    /** Возвращает страницу сборов группы. */
    PageResult<Event> listByGroup(long groupId, long requesterId, PageRequest pageRequest);

    /** Помечает сбор удалённым. */
    void delete(long eventId, long requesterId);
}
