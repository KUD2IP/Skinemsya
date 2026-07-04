package skinemsya.vse.ru.receipts.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.event.SelectionsCompleted;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.application.EventServiceImpl;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.events.domain.exception.EventNotInDistributionException;
import skinemsya.vse.ru.files.application.FileService;
import skinemsya.vse.ru.integrations.application.MlServiceClient;
import skinemsya.vse.ru.integrations.domain.MlReceiptItem;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;
import skinemsya.vse.ru.receipts.domain.PositionSource;
import skinemsya.vse.ru.receipts.domain.Receipt;
import skinemsya.vse.ru.receipts.domain.ReceiptStatus;
import skinemsya.vse.ru.receipts.domain.exception.ReceiptNotFoundException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.ReceiptEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.ReceiptRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ReceiptServiceImpl implements ReceiptService {

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.5;

    private final ReceiptRepository receiptRepository;
    private final PositionRepository positionRepository;
    private final FileService fileService;
    private final MlServiceClient mlServiceClient;
    private final EventAccessPort eventAccessPort;
    private final ObjectMapper objectMapper;

    public ReceiptServiceImpl(
            ReceiptRepository receiptRepository,
            PositionRepository positionRepository,
            FileService fileService,
            MlServiceClient mlServiceClient,
            EventAccessPort eventAccessPort,
            ObjectMapper objectMapper
    ) {
        this.receiptRepository = receiptRepository;
        this.positionRepository = positionRepository;
        this.fileService = fileService;
        this.mlServiceClient = mlServiceClient;
        this.eventAccessPort = eventAccessPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Receipt processReceipt(long eventId, long fileId, long userId) {
        requireDraft(eventId);
        eventAccessPort.requireParticipant(eventId, userId);

        var file = fileService.requireById(fileId);
        byte[] imageBytes;
        try (var stream = fileService.getContent(fileId, userId, false)) {
            imageBytes = stream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read receipt file", ex);
        }

        var receipt = new ReceiptEntity();
        receipt.setEventId(eventId);
        receipt.setFileId(fileId);
        receipt.setStatus(ReceiptStatus.PROCESSING);
        receipt.setCreatedAt(Instant.now());
        receipt = receiptRepository.save(receipt);

        MlReceiptResponse mlResponse;
        try {
            mlResponse = mlServiceClient.recognize(imageBytes, file.mimeType());
            receipt.setMlRawJson(objectMapper.writeValueAsString(mlResponse));
        } catch (Exception ex) {
            receipt.setStatus(ReceiptStatus.FAILED);
            receiptRepository.save(receipt);
            return toDomain(receipt);
        }

        if (mlResponse.items() == null || mlResponse.items().isEmpty()) {
            receipt.setStatus(ReceiptStatus.FAILED);
            receiptRepository.save(receipt);
            return toDomain(receipt);
        }

        double confidence = mlResponse.confidence() != null ? mlResponse.confidence() : 0.0;
        if (confidence < LOW_CONFIDENCE_THRESHOLD) {
            receipt.setStatus(ReceiptStatus.FAILED);
            receiptRepository.save(receipt);
            return toDomain(receipt);
        }

        boolean lowConfidence = confidence < 0.7;
        for (MlReceiptItem item : mlResponse.items()) {
            if (item.name() == null || item.name().isBlank() || item.quantity() <= 0 || item.totalPrice() < 0) {
                continue;
            }
            var position = new PositionEntity();
            position.setEventId(eventId);
            position.setReceiptId(receipt.getId());
            position.setName(item.name().trim());
            position.setQuantity(BigDecimal.valueOf(item.quantity()).setScale(2, RoundingMode.HALF_UP));
            position.setTotalPriceKopecks(Math.round(item.totalPrice() * 100));
            position.setSource(PositionSource.RECEIPT);
            position.setTips(isTips(item.name()));
            position.setLowConfidence(lowConfidence || (item.confidence() != null && item.confidence() < 0.7));
            position.setCreatedAt(Instant.now());
            positionRepository.save(position);
        }

        receipt.setStatus(ReceiptStatus.PROCESSED);
        receiptRepository.save(receipt);
        return toDomain(receipt);
    }

    @Override
    public Receipt splitTips(long eventId, long receiptId, long userId) {
        requireDraft(eventId);
        eventAccessPort.requireParticipant(eventId, userId);
        var receipt = receiptRepository.findByIdAndEventId(receiptId, eventId)
                .orElseThrow(ReceiptNotFoundException::new);

        var tipsPositions = positionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .filter(PositionEntity::isTips)
                .toList();
        for (var position : tipsPositions) {
            position.setShared(true);
            positionRepository.save(position);
        }
        return toDomain(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> listByEvent(long eventId, long userId) {
        eventAccessPort.requireParticipant(eventId, userId);
        return receiptRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(ReceiptServiceImpl::toDomain)
                .toList();
    }

    private static boolean isTips(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.contains("tips") || lower.contains("чаевые") || lower.contains("service");
    }

    private void requireDraft(long eventId) {
        if (eventAccessPort.getStatus(eventId) != EventStatus.DRAFT) {
            throw new skinemsya.vse.ru.events.domain.exception.EventNotDraftException();
        }
    }

    private static Receipt toDomain(ReceiptEntity entity) {
        return new Receipt(entity.getId(), entity.getEventId(), entity.getFileId(), entity.getStatus(), entity.getCreatedAt());
    }
}
