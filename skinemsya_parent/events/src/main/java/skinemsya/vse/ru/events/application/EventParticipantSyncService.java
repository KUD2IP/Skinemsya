package skinemsya.vse.ru.events.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.EventParticipantsChanged;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.infrastructure.persistence.EventEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;
import skinemsya.vse.ru.groups.application.GroupAccessService;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class EventParticipantSyncService {

    private static final Set<EventStatus> ACTIVE_STATUSES = EnumSet.of(
            EventStatus.DRAFT,
            EventStatus.DISTRIBUTION,
            EventStatus.CALCULATED
    );

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final GroupAccessService groupAccessService;
    private final ApplicationEventPublisher eventPublisher;

    public EventParticipantSyncService(
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            GroupAccessService groupAccessService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.groupAccessService = groupAccessService;
        this.eventPublisher = eventPublisher;
    }

    public void syncMemberToActiveEvents(long groupId, long userId) {
        if (!groupAccessService.isMember(groupId, userId)) {
            return;
        }
        for (EventEntity event : findActiveEvents(groupId)) {
            if (addParticipantIfAbsent(event, userId)) {
                eventPublisher.publishEvent(new EventParticipantsChanged(event.getId(), groupId));
            }
        }
    }

    public void syncAllMembersBeforeDistribution(long eventId, long groupId) {
        var event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }
        for (long memberUserId : groupAccessService.memberUserIds(groupId)) {
            if (addParticipantIfAbsent(event, memberUserId)) {
                eventPublisher.publishEvent(new EventParticipantsChanged(eventId, groupId));
            }
        }
    }

    private List<EventEntity> findActiveEvents(long groupId) {
        return eventRepository.findByGroupIdAndDeletedAtIsNullAndStatusIn(groupId, ACTIVE_STATUSES);
    }

    private boolean addParticipantIfAbsent(EventEntity event, long userId) {
        long eventId = event.getId();
        if (eventParticipantRepository.existsByEventIdAndUserId(eventId, userId)) {
            return false;
        }
        var participant = new EventParticipantEntity();
        participant.setEventId(eventId);
        participant.setUserId(userId);
        eventParticipantRepository.save(participant);
        return true;
    }
}
