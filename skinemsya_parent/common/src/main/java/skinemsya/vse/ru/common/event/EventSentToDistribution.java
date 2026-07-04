package skinemsya.vse.ru.common.event;

public record EventSentToDistribution(
        long eventId,
        long groupId,
        long payerId,
        String eventTitle,
        long totalKopecks
) {
}
