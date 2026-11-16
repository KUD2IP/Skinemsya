package skinemsya.vse.ru.api.internal.event.events;

/** Сбор закрыт плательщиком. */
public record EventCompleted(long eventId, long groupId, String eventTitle) {}
