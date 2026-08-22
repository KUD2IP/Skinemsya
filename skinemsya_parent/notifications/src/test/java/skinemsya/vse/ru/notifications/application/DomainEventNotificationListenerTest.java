package skinemsya.vse.ru.notifications.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.common.event.DebtorConfirmed;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.groups.application.GroupService;
import skinemsya.vse.ru.groups.domain.Group;
import skinemsya.vse.ru.groups.domain.GroupType;
import skinemsya.vse.ru.notifications.domain.NotificationType;
import skinemsya.vse.ru.users.application.UserService;

@ExtendWith(MockitoExtension.class)
class DomainEventNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EventAccessPort eventAccessPort;

    @Mock
    private GroupService groupService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DomainEventNotificationListener listener;

    @Test
    void shouldAnnounceDebtorConfirmedInGroupChatAndNotSendPrivateMessage() {
        when(eventAccessPort.getEventGroupId(10L)).thenReturn(20L);
        when(groupService.findById(20L)).thenReturn(Optional.of(group(-100L)));

        listener.onDebtorConfirmed(new DebtorConfirmed(10L, 5L, 2L, 1L, 15_000L, "Анна"));

        verify(notificationService).sendToGroupChat(-100L, NotificationType.PAYMENT_PENDING, "Анна скинул");
        verify(notificationService, never()).sendToGroupChat(anyLong(), any(), anyString(), anyLong());
        verify(notificationService, never()).send(anyLong(), any(), anyString());
        verify(notificationService, never()).sendToEventParticipants(anyLong(), any(), anyString());
    }

    @Test
    void shouldSkipDebtorConfirmedWhenGroupHasNoChat() {
        when(eventAccessPort.getEventGroupId(10L)).thenReturn(20L);
        when(groupService.findById(20L)).thenReturn(Optional.of(group(null)));

        listener.onDebtorConfirmed(new DebtorConfirmed(10L, 5L, 2L, 1L, 15_000L, "Анна"));

        verify(notificationService, never()).sendToGroupChat(anyLong(), any(), anyString());
        verify(notificationService, never()).sendToGroupChat(anyLong(), any(), anyString(), anyLong());
        verify(notificationService, never()).send(anyLong(), any(), anyString());
    }

    private static Group group(Long telegramChatId) {
        var now = Instant.now();
        return new Group(20L, "Friends", GroupType.CHAT_LINKED, telegramChatId, 1L, now, now);
    }
}
