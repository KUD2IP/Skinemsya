package skinemsya.vse.ru.events.application;

import skinemsya.vse.ru.common.api.PageRequest;
import skinemsya.vse.ru.common.api.PageResult;
import skinemsya.vse.ru.events.domain.Event;

import java.util.Optional;

public interface EventService {

    Event create(long groupId, String name, String description, long payerId, long creatorId);

    Event update(long eventId, long requesterId, String name, String description, long payerId);

    Optional<Event> findById(long eventId);

    PageResult<Event> listByGroup(long groupId, long requesterId, PageRequest pageRequest);

    void delete(long eventId, long requesterId);
}
