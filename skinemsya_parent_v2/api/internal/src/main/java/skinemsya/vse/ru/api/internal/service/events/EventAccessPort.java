package skinemsya.vse.ru.api.internal.service.events;

import java.util.List;
import skinemsya.vse.ru.api.internal.enums.events.EventStatus;
import skinemsya.vse.ru.api.internal.pojo.events.Event;

/** Операции со сбором, нужные соседним доменам. */
public interface EventAccessPort {

    /** Возвращает идентификатор группы сбора. */
    long getEventGroupId(long eventId);

    /** Возвращает код приглашения сбора. */
    String inviteCode(long eventId);

    /** Возвращает идентификатор плательщика. */
    long getPayerId(long eventId);

    /** Возвращает статус сбора. */
    EventStatus getStatus(long eventId);

    /** Требует, чтобы пользователь был участником сбора. */
    void requireParticipant(long eventId, long userId);

    /** Возвращает число участников сбора. */
    long countParticipants(long eventId);

    /** Возвращает ожидаемое число участников. */
    int getExpectedParticipantCount(long eventId);

    /** Проверяет, состоит ли пользователь в сборе. */
    boolean isParticipant(long eventId, long userId);

    /** Возвращает идентификаторы участников по возрастанию. */
    List<Long> getParticipantUserIds(long eventId);

    /** Переводит сбор из распределения в рассчитанный. */
    void markCalculated(long eventId);

    /** Закрывает рассчитанный сбор от имени плательщика. */
    Event closeByPayer(long eventId, long payerId);

    /** Отмечает, что участник закончил выбор позиций. */
    void markSelectionCompleted(long eventId, long userId);

    /** Сбрасывает отметку завершения выбора. */
    void clearSelectionCompleted(long eventId, long userId);

    /** Проверяет, что все участники закончили выбор. */
    boolean allSelectionsCompleted(long eventId);

    /** Возвращает участников, закончивших выбор. */
    List<Long> getSelectionCompletedParticipantUserIds(long eventId);

    /** Возвращает участников, не закончивших выбор. */
    List<Long> getIncompleteSelectionParticipantUserIds(long eventId);

    /** Возвращает рассчитанный сбор обратно в распределение. */
    void revertCalculatedToDistribution(long eventId);

    /** Отправляет черновик на распределение. */
    Event sendToDistribution(long eventId, long requesterId);
}
