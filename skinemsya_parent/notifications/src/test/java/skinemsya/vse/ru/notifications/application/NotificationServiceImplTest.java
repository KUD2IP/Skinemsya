package skinemsya.vse.ru.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.debts.application.DebtService;
import skinemsya.vse.ru.debts.domain.Debt;
import skinemsya.vse.ru.debts.domain.DebtStatus;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantEntity;
import skinemsya.vse.ru.events.infrastructure.persistence.EventParticipantRepository;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupType;
import skinemsya.vse.ru.integrations.application.TelegramBotClient;
import skinemsya.vse.ru.notifications.infrastructure.persistence.NotificationRepository;
import skinemsya.vse.ru.users.application.UserService;
import skinemsya.vse.ru.users.domain.User;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final long EVENT_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long CHAT_ID = -100L;
    private static final long REQUESTER_ID = 1L;
    private static final long PAYER_ID = 1L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TelegramBotClient telegramBotClient;

    @Mock
    private EventAccessPort eventAccessPort;

    @Mock
    private EventParticipantRepository eventParticipantRepository;

    @Mock
    private GroupService groupService;

    @Mock
    private UserService userService;

    @Mock
    private DebtService debtService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void shouldRemindAllIncompleteParticipantsInOneMessage() {
        stubGroupChat(EventStatus.DISTRIBUTION);
        when(eventAccessPort.getPayerId(EVENT_ID)).thenReturn(PAYER_ID);
        when(eventParticipantRepository.findByEventId(EVENT_ID))
                .thenReturn(List.of(
                        participant(PAYER_ID, null),
                        participant(2L, null),
                        participant(3L, null),
                        participant(4L, Instant.now())));
        when(userService.findById(2L)).thenReturn(Optional.of(user(2L, "Иван", "ivan")));
        when(userService.findById(3L)).thenReturn(Optional.of(user(3L, "Петр", null)));

        notificationService.remindIncompleteSelections(EVENT_ID, REQUESTER_ID);

        verify(telegramBotClient)
                .sendMessageWithOpenAppButton(
                        eq(CHAT_ID),
                        eq("Ждём выбор позиций от @ivan и Петр"),
                        eq("Скинуть"),
                        eq("supergroup"),
                        eq("event_" + EVENT_ID));
    }

    @Test
    void shouldRemindOnlyUnpaidDebtorsWhenCalculated() {
        stubGroupChat(EventStatus.CALCULATED);
        when(debtService.findByEvent(EVENT_ID))
                .thenReturn(List.of(
                        debt(2L, DebtStatus.UNPAID),
                        debt(3L, DebtStatus.UNPAID),
                        debt(4L, DebtStatus.PENDING_CONFIRMATION),
                        debt(5L, DebtStatus.PAID)));
        when(userService.findById(2L)).thenReturn(Optional.of(user(2L, "Иван", "ivan")));
        when(userService.findById(3L)).thenReturn(Optional.of(user(3L, "Анна", "anna")));

        notificationService.remindIncompleteSelections(EVENT_ID, REQUESTER_ID);

        verify(telegramBotClient)
                .sendMessageWithOpenAppButton(
                        eq(CHAT_ID),
                        eq("Ждём перевод от @ivan и @anna"),
                        eq("Скинуть"),
                        eq("supergroup"),
                        eq("event_" + EVENT_ID));
        verify(eventParticipantRepository, never()).findByEventId(anyLong());
    }

    @Test
    void shouldSkipRemindWhenStandaloneGroupHasNoChat() {
        when(eventAccessPort.getEventGroupId(EVENT_ID)).thenReturn(GROUP_ID);
        when(groupService.findById(GROUP_ID)).thenReturn(Optional.of(group(null)));

        notificationService.remindIncompleteSelections(EVENT_ID, REQUESTER_ID);

        verify(telegramBotClient, never())
                .sendMessageWithOpenAppButton(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldFormatThreeMentionsWithCommaAndConjunction() {
        assertThat(NotificationServiceImpl.formatMentions(List.of("@ivan", "@anna", "Петр")))
                .isEqualTo("@ivan, @anna и Петр");
    }

    @Test
    void shouldSendSingleGroupMessageForThreeIncompleteParticipants() {
        stubGroupChat(EventStatus.DISTRIBUTION);
        when(eventAccessPort.getPayerId(EVENT_ID)).thenReturn(PAYER_ID);
        when(eventParticipantRepository.findByEventId(EVENT_ID))
                .thenReturn(List.of(participant(2L, null), participant(3L, null), participant(4L, null)));
        when(userService.findById(2L)).thenReturn(Optional.of(user(2L, "Иван", "ivan")));
        when(userService.findById(3L)).thenReturn(Optional.of(user(3L, "Анна", "anna")));
        when(userService.findById(4L)).thenReturn(Optional.of(user(4L, "Петр", null)));

        notificationService.remindIncompleteSelections(EVENT_ID, REQUESTER_ID);

        var message = ArgumentCaptor.forClass(String.class);
        verify(telegramBotClient)
                .sendMessageWithOpenAppButton(
                        eq(CHAT_ID), message.capture(), eq("Скинуть"), eq("supergroup"), eq("event_" + EVENT_ID));
        assertThat(message.getValue()).isEqualTo("Ждём выбор позиций от @ivan, @anna и Петр");
    }

    private void stubGroupChat(EventStatus status) {
        when(eventAccessPort.getEventGroupId(EVENT_ID)).thenReturn(GROUP_ID);
        when(groupService.findById(GROUP_ID)).thenReturn(Optional.of(group(CHAT_ID)));
        when(eventAccessPort.getStatus(EVENT_ID)).thenReturn(status);
    }

    private static Group group(Long telegramChatId) {
        var now = Instant.now();
        return new Group(GROUP_ID, "Friends", GroupType.CHAT_LINKED, telegramChatId, PAYER_ID, now, now);
    }

    private static EventParticipantEntity participant(long userId, Instant completedAt) {
        var entity = new EventParticipantEntity();
        entity.setEventId(EVENT_ID);
        entity.setUserId(userId);
        entity.setSelectionCompletedAt(completedAt);
        return entity;
    }

    private static User user(long id, String displayName, String username) {
        var now = Instant.now();
        return new User(id, 1000 + id, displayName, username, now, now);
    }

    private static Debt debt(long debtorId, DebtStatus status) {
        var now = Instant.now();
        return new Debt(debtorId, EVENT_ID, debtorId, PAYER_ID, 15_000L, status, now, now);
    }
}
