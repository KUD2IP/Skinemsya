package skinemsya.vse.ru.events.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.events.domain.exception.EventCannotJoinException;
import skinemsya.vse.ru.events.domain.exception.EventFullException;
import skinemsya.vse.ru.events.domain.exception.EventNotFoundException;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;
import skinemsya.vse.ru.groups.application.GroupAccessService;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.groups.domain.exception.GroupNotFoundException;

@Service
@Transactional
public class EventDeepLinkAccessService {

    private final EventRepository eventRepository;
    private final GroupService groupService;
    private final GroupAccessService groupAccessService;
    private final EventService eventService;

    public EventDeepLinkAccessService(
            EventRepository eventRepository,
            GroupService groupService,
            GroupAccessService groupAccessService,
            EventService eventService) {
        this.eventRepository = eventRepository;
        this.groupService = groupService;
        this.groupAccessService = groupAccessService;
        this.eventService = eventService;
    }

    public void ensureAccess(long eventId, long userId) {
        var event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
        var group = groupService.findById(event.getGroupId()).orElseThrow(GroupNotFoundException::new);

        if (!groupAccessService.isMember(group.id(), userId)) {
            groupService.joinFromInvite(group.id(), userId);
        }

        if (groupAccessService.isMember(group.id(), userId)) {
            try {
                eventService.join(eventId, userId);
            } catch (EventFullException | EventCannotJoinException ignored) {
                // User can open the event as a group member without a seat.
            }
        }
    }
}
