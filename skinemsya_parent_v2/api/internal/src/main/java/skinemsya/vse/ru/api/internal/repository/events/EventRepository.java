package skinemsya.vse.ru.api.internal.repository.events;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.pojo.common.PageRequest;
import skinemsya.vse.ru.api.internal.pojo.common.PageResult;
import skinemsya.vse.ru.api.internal.pojo.events.Event;

/** Порт хранения сборов. */
public interface EventRepository {

    /** Находит активный сбор по идентификатору. */
    Optional<Event> findById(long eventId);

    /** Находит активный сбор по коду приглашения. */
    Optional<Event> findByInviteCode(String inviteCode);

    /** Возвращает активные сборы группы от новых к старым. */
    List<Event> findByGroupIdOrderByCreatedAtDesc(long groupId);

    /** Возвращает страницу активных сборов группы от новых к старым. */
    PageResult<Event> findByGroupIdOrderByCreatedAtDesc(long groupId, PageRequest pageRequest);

    /** Проверяет, есть ли у группы активный сбор. */
    boolean existsByGroupId(long groupId);

    /** Проверяет, есть ли у группы активный сбор в статусе, отличном от заданного. */
    boolean existsByGroupIdAndStatusNot(long groupId, EventStatus status);

    /** Проверяет, есть ли активный сбор группы с этим плательщиком. */
    boolean existsByGroupIdAndPayerId(long groupId, long payerId);

    /** Возвращает активные сборы группы в заданном статусе. */
    List<Event> findByGroupIdAndStatus(long groupId, EventStatus status);

    /** Возвращает активные сборы группы в одном из статусов. */
    List<Event> findByGroupIdAndStatusIn(long groupId, Collection<EventStatus> statuses);

    /** Сохраняет сбор. */
    Event save(Event event);

    /** Помечает сбор удалённым. */
    void markDeleted(long eventId, Instant deletedAt, Instant updatedAt);
}
