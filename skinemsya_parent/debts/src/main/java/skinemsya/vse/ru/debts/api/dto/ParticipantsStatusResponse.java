package skinemsya.vse.ru.debts.api.dto;

import java.util.List;

public record ParticipantsStatusResponse(
        long totalParticipants,
        long completedSelections,
        List<ParticipantStatusItem> participants
) {
    public record ParticipantStatusItem(
            long userId,
            boolean selectionCompleted,
            String debtStatus
    ) {
    }
}
