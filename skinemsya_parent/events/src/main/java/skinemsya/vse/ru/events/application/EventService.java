package skinemsya.vse.ru.events.application;

import java.util.Optional;
import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.events.domain.Event;

public interface EventService {

    Event create(
            long groupId,
            String name,
            String description,
            long payerId,
            long creatorId,
            int expectedParticipantCount);

    Event update(
            long eventId,
            long requesterId,
            String name,
            String description,
            long payerId,
            int expectedParticipantCount);

    Event join(long eventId, long userId);

    Event leave(long eventId, long userId);

    Event removeParticipant(long eventId, long requesterId, long targetUserId);

    Event updateExpectedParticipantCount(long eventId, long requesterId, int expectedParticipantCount);

    Optional<Event> findById(long eventId);

    PageResult<Event> listByGroup(long groupId, long requesterId, PageRequest pageRequest);

    void delete(long eventId, long requesterId);
}
