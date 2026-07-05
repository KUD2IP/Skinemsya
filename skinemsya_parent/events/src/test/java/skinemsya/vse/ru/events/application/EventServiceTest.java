package skinemsya.vse.ru.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.common.domain.ErrorCode;
import skinemsya.vse.ru.events.domain.Event;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.PayerNotGroupMemberException;
import skinemsya.vse.ru.events.infrastructure.mapper.EventMapper;
import skinemsya.vse.ru.events.infrastructure.persistence.EventEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;
import skinemsya.vse.ru.events.infrastructure.persistence.EventRepository;
import skinemsya.vse.ru.groups.application.GroupAccessService;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.User;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private GroupAccessService groupAccessService;

    @Mock
    private UserService userService;

    @Mock
    private EventParticipantSyncService eventParticipantSyncService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private DistributionReadinessPort distributionReadinessPort;

    @InjectMocks
    private EventServiceImpl eventService;

    private static final long GROUP_ID = 10L;
    private static final long CREATOR_ID = 1L;
    private static final long PAYER_ID = 1L;
    private static final long OTHER_MEMBER_ID = 2L;

    @Test
    void shouldCreateDraftEventWithGroupMembersAsParticipants() {
        var saved = eventEntity(100L, EventStatus.DRAFT);
        var domain = domainEvent(100L, EventStatus.DRAFT);

        when(userService.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID)));
        when(userService.findById(PAYER_ID)).thenReturn(Optional.of(user(PAYER_ID)));
        when(groupAccessService.isMember(GROUP_ID, PAYER_ID)).thenReturn(true);
        when(groupAccessService.memberUserIds(GROUP_ID)).thenReturn(List.of(CREATOR_ID, OTHER_MEMBER_ID));
        when(eventRepository.save(any(EventEntity.class))).thenReturn(saved);
        when(eventMapper.toDomain(saved)).thenReturn(domain);

        var result = eventService.create(GROUP_ID, "Dinner", "Desc", PAYER_ID, CREATOR_ID);

        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(result.payerId()).isEqualTo(PAYER_ID);
        verify(groupAccessService).requireMember(GROUP_ID, CREATOR_ID);
        verify(eventParticipantRepository, times(2)).save(any(EventParticipantEntity.class));
    }

    @Test
    void shouldRejectCreateWhenPayerNotGroupMember() {
        when(userService.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID)));
        when(userService.findById(OTHER_MEMBER_ID)).thenReturn(Optional.of(user(OTHER_MEMBER_ID)));
        when(groupAccessService.isMember(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> eventService.create(GROUP_ID, "Dinner", null, OTHER_MEMBER_ID, CREATOR_ID))
                .isInstanceOf(PayerNotGroupMemberException.class)
                .extracting(ex -> ((PayerNotGroupMemberException) ex).errorCode())
                .isEqualTo(ErrorCode.DOMAIN_RULE_VIOLATION);
    }

    @Test
    void shouldUpdatePayerInDraftEvent() {
        var existing = eventEntity(100L, EventStatus.DRAFT);
        var updated = domainEvent(100L, EventStatus.DRAFT);

        when(eventRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(groupAccessService.isMember(GROUP_ID, OTHER_MEMBER_ID)).thenReturn(true);
        when(userService.findById(OTHER_MEMBER_ID)).thenReturn(Optional.of(user(OTHER_MEMBER_ID)));
        when(eventRepository.save(existing)).thenReturn(existing);
        when(eventMapper.toDomain(existing)).thenReturn(updated);

        eventService.update(100L, CREATOR_ID, "Dinner", "New desc", OTHER_MEMBER_ID);

        assertThat(existing.getPayerId()).isEqualTo(OTHER_MEMBER_ID);
    }

    private static User user(long id) {
        return new User(id, 100_000L + id, "User " + id, null, Instant.now(), Instant.now());
    }

    private static EventEntity eventEntity(long id, EventStatus status) {
        var entity = new EventEntity();
        entity.setId(id);
        entity.setGroupId(GROUP_ID);
        entity.setName("Dinner");
        entity.setPayerId(PAYER_ID);
        entity.setCreatedBy(CREATOR_ID);
        entity.setStatus(status);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private static Event domainEvent(long id, EventStatus status) {
        return new Event(id, GROUP_ID, "Dinner", "Desc", PAYER_ID, CREATOR_ID, status, Instant.now(), Instant.now());
    }
}
