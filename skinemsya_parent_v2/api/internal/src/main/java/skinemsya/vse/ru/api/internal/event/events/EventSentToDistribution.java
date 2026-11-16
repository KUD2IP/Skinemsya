package skinemsya.vse.ru.api.internal.event.events;

/** Сбор отправлен на распределение позиций. */
public record EventSentToDistribution(long eventId, long groupId, long payerId, String eventTitle, long totalKopecks) {}
