package skinemsya.vse.ru.events.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import skinemsya.vse.ru.common.event.EventParticipantsChanged;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.infrastructure.persistence.EventEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;
import skinemsya.vse.ru.groups.application.GroupAccessService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventParticipantSyncServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private GroupAccessService groupAccessService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EventParticipantSyncService syncService;

    @Test
    void shouldAddMemberToActiveEvents() {
        when(groupAccessService.isMember(10L, 3L)).thenReturn(true);
        when(eventRepository.findByGroupIdAndDeletedAtIsNullAndStatusIn(eq(10L), any()))
                .thenReturn(List.of(activeEvent(100L)));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, 3L)).thenReturn(false);

        syncService.syncMemberToActiveEvents(10L, 3L);

        verify(eventParticipantRepository).save(any(EventParticipantEntity.class));
        verify(eventPublisher).publishEvent(new EventParticipantsChanged(100L, 10L));
    }

    @Test
    void shouldSyncAllGroupMembersBeforeDistribution() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(activeEvent(100L)));
        when(groupAccessService.memberUserIds(10L)).thenReturn(List.of(1L, 2L));
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, 1L)).thenReturn(true);
        when(eventParticipantRepository.existsByEventIdAndUserId(100L, 2L)).thenReturn(false);

        syncService.syncAllMembersBeforeDistribution(100L, 10L);

        verify(eventParticipantRepository).save(any(EventParticipantEntity.class));
        verify(eventPublisher).publishEvent(new EventParticipantsChanged(100L, 10L));
    }

    @Test
    void shouldSkipWhenUserNotGroupMember() {
        when(groupAccessService.isMember(10L, 3L)).thenReturn(false);

        syncService.syncMemberToActiveEvents(10L, 3L);

        verify(eventParticipantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static EventEntity activeEvent(long id) {
        var entity = new EventEntity();
        entity.setId(id);
        entity.setGroupId(10L);
        entity.setStatus(EventStatus.DISTRIBUTION);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
