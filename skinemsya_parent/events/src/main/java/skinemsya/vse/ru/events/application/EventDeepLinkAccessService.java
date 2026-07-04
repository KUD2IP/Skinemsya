package skinemsya.vse.ru.events.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.events.domain.exception.EventNotFoundException;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;
import skinemsya.vse.ru.groups.application.GroupAccessService;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.groups.domain.GroupType;
import skinemsya.vse.ru.groups.domain.exception.GroupNotFoundException;

@Service
@Transactional
public class EventDeepLinkAccessService {

    private final EventRepository eventRepository;
    private final GroupService groupService;
    private final GroupAccessService groupAccessService;
    private final EventParticipantSyncService eventParticipantSyncService;

    public EventDeepLinkAccessService(
            EventRepository eventRepository,
            GroupService groupService,
            GroupAccessService groupAccessService,
            EventParticipantSyncService eventParticipantSyncService
    ) {
        this.eventRepository = eventRepository;
        this.groupService = groupService;
        this.groupAccessService = groupAccessService;
        this.eventParticipantSyncService = eventParticipantSyncService;
    }

    public void ensureAccess(long eventId, long userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);
        var group = groupService.findById(event.getGroupId())
                .orElseThrow(GroupNotFoundException::new);

        if (!groupAccessService.isMember(group.id(), userId)) {
            if (group.type() == GroupType.CHAT_LINKED) {
                groupService.joinChatLinkedGroup(group.id(), userId);
            }
        }

        if (groupAccessService.isMember(group.id(), userId)) {
            eventParticipantSyncService.syncMemberToActiveEvents(group.id(), userId);
        }
    }
}
