package skinemsya.vse.ru.api.internal.event.events;

/** Состав участников сбора изменился. */
public record EventParticipantsChanged(long eventId, long groupId) {}
