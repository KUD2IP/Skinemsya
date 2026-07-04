package skinemsya.vse.ru.receipts.application;

import org.springframework.stereotype.Component;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.files.application.FileSharedAccessVerifier;
import skinemsya.vse.ru.receipts.infrastructure.persistence.ReceiptRepository;

@Component
public class ReceiptEventFileAccessVerifier implements FileSharedAccessVerifier {

    private final ReceiptRepository receiptRepository;
    private final EventAccessPort eventAccessPort;

    public ReceiptEventFileAccessVerifier(
            ReceiptRepository receiptRepository,
            EventAccessPort eventAccessPort
    ) {
        this.receiptRepository = receiptRepository;
        this.eventAccessPort = eventAccessPort;
    }

    @Override
    public boolean canAccess(long fileId, long requesterId) {
        for (var receipt : receiptRepository.findByFileId(fileId)) {
            try {
                eventAccessPort.requireParticipant(receipt.getEventId(), requesterId);
                return true;
            } catch (RuntimeException ignored) {
                // try next receipt linked to same file
            }
        }
        return false;
    }
}
