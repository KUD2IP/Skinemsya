package skinemsya.vse.ru.events.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.GroupMemberJoined;

@Component
public class GroupMemberJoinedEventListener {

    private final EventParticipantSyncService eventParticipantSyncService;

    public GroupMemberJoinedEventListener(EventParticipantSyncService eventParticipantSyncService) {
        this.eventParticipantSyncService = eventParticipantSyncService;
    }

    @EventListener
    @Transactional
    public void onGroupMemberJoined(GroupMemberJoined event) {
        eventParticipantSyncService.syncMemberToActiveEvents(event.groupId(), event.userId());
    }
}
