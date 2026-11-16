package skinemsya.vse.ru.events.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.groups.application.GroupService;

@Service
public class OneOffEventService {

    private final GroupService groupService;
    private final EventService eventService;

    public OneOffEventService(GroupService groupService, EventService eventService) {
        this.groupService = groupService;
        this.eventService = eventService;
    }

    @Transactional
    public Event create(String name, String description, long creatorId, int expectedParticipantCount) {
        var group = groupService.createOneOff(name, creatorId);
        return eventService.create(group.id(), name, description, creatorId, creatorId, expectedParticipantCount);
    }
}
