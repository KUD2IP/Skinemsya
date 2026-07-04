package skinemsya.vse.ru.notifications.application;

import skinemsya.vse.ru.notifications.domain.Notification;
import skinemsya.vse.ru.notifications.domain.NotificationType;

public interface NotificationService {

    Notification send(long userId, NotificationType type, String payload);

    void sendToEventParticipants(long eventId, NotificationType type, String payload);

    void sendToGroupChat(long telegramChatId, NotificationType type, String message, long eventId);

    void remindIncompleteSelections(long eventId, long requesterId);
}
