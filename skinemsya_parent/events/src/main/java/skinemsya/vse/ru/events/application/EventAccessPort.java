package skinemsya.vse.ru.events.application;

import java.util.List;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.events.domain.EventStatus;

public interface EventAccessPort {

    long getEventGroupId(long eventId);

    long getPayerId(long eventId);

    EventStatus getStatus(long eventId);

    void requireParticipant(long eventId, long userId);

    long countParticipants(long eventId);

    List<Long> getParticipantUserIds(long eventId);

    void markCalculated(long eventId);

    boolean tryMarkCompleted(long eventId);

    void markSelectionCompleted(long eventId, long userId);

    boolean allSelectionsCompleted(long eventId);

    List<Long> getSelectionCompletedParticipantUserIds(long eventId);

    void revertCalculatedToDistribution(long eventId);

    Event sendToDistribution(long eventId, long requesterId);
}
