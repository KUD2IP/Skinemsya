package skinemsya.vse.ru.receipts.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.common.domain.DomainException;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.files.application.FileService;
import skinemsya.vse.ru.integrations.application.MlServiceClient;
import skinemsya.vse.ru.integrations.domain.MlReceiptItem;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;
import skinemsya.vse.ru.integrations.domain.ReceiptLineReconciler;
import skinemsya.vse.ru.receipts.domain.PositionSource;
import skinemsya.vse.ru.receipts.domain.Receipt;
import skinemsya.vse.ru.receipts.domain.ReceiptStatus;
import skinemsya.vse.ru.receipts.domain.exception.ReceiptNotFoundException;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.ReceiptEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.ReceiptRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.SharedPositionTargetRepository;

@Service
@Transactional
public class ReceiptServiceImpl implements ReceiptService {

    private static final double ITEM_LOW_CONFIDENCE = 0.7;

    private final ReceiptRepository receiptRepository;
    private final PositionRepository positionRepository;
    private final PositionSelectionRepository selectionRepository;
    private final SharedPositionTargetRepository sharedTargetRepository;
    private final FileService fileService;
    private final MlServiceClient mlServiceClient;
    private final EventAccessPort eventAccessPort;
    private final ReceiptParseAccess receiptParseAccess;
    private final ObjectMapper objectMapper;

    public ReceiptServiceImpl(
            ReceiptRepository receiptRepository,
            PositionRepository positionRepository,
            PositionSelectionRepository selectionRepository,
            SharedPositionTargetRepository sharedTargetRepository,
            FileService fileService,
            MlServiceClient mlServiceClient,
            EventAccessPort eventAccessPort,
            ReceiptParseAccess receiptParseAccess,
            ObjectMapper objectMapper) {
        this.receiptRepository = receiptRepository;
        this.positionRepository = positionRepository;
        this.selectionRepository = selectionRepository;
        this.sharedTargetRepository = sharedTargetRepository;
        this.fileService = fileService;
        this.mlServiceClient = mlServiceClient;
        this.eventAccessPort = eventAccessPort;
        this.receiptParseAccess = receiptParseAccess;
        this.objectMapper = objectMapper;
    }

    @Override
    public Receipt processReceipt(long eventId, long fileId, long userId) {
        requireDraft(eventId);
        eventAccessPort.requireParticipant(eventId, userId);
        receiptParseAccess.authorizeParse(userId);

        var file = fileService.requireById(fileId);
        byte[] imageBytes;
        try (var stream = fileService.getContent(fileId, userId, false)) {
            imageBytes = stream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read receipt file", ex);
        }

        String imageSha256 = sha256(imageBytes);
        var receipt = new ReceiptEntity();
        receipt.setEventId(eventId);
        receipt.setFileId(fileId);
        receipt.setStatus(ReceiptStatus.PROCESSING);
        receipt.setImageSha256(imageSha256);
        receipt.setCreatedAt(Instant.now());
        receipt = receiptRepository.save(receipt);

        MlReceiptResponse mlResponse;
        try {
            mlResponse = ReceiptLineReconciler.reconcile(
                    resolveRecognition(imageSha256, imageBytes, file.mimeType(), userId));
            receipt.setMlRawJson(objectMapper.writeValueAsString(mlResponse));
        } catch (DomainException ex) {
            receipt.setStatus(ReceiptStatus.FAILED);
            receiptRepository.save(receipt);
            throw ex;
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

        long itemsSum = 0;
        int saved = 0;
        for (MlReceiptItem item : mlResponse.items()) {
            if (item.name() == null || item.name().isBlank() || item.quantity() <= 0 || item.totalPrice() < 0) {
                continue;
            }
            long lineKopecks = toKopecks(item.totalPrice());
            var position = new PositionEntity();
            position.setEventId(eventId);
            position.setReceiptId(receipt.getId());
            position.setName(item.name().trim());
            position.setQuantity(BigDecimal.valueOf(item.quantity()).setScale(2, RoundingMode.HALF_UP));
            position.setTotalPriceKopecks(lineKopecks);
            position.setSource(PositionSource.RECEIPT);
            position.setTips(isTips(item.name()));
            position.setLowConfidence(item.confidence() == null || item.confidence() < ITEM_LOW_CONFIDENCE);
            position.setCreatedAt(Instant.now());
            positionRepository.save(position);
            itemsSum += lineKopecks;
            saved++;
        }

        if (saved == 0) {
            receipt.setStatus(ReceiptStatus.FAILED);
            receiptRepository.save(receipt);
            return toDomain(receipt);
        }

        receipt.setItemsSumKopecks(itemsSum);
        receipt.setTotalKopecks(mlResponse.total() == null ? null : toKopecks(mlResponse.total()));
        receipt.setStatus(ReceiptStatus.PROCESSED);
        receiptRepository.save(receipt);
        return toDomain(receipt);
    }

    @Override
    public Receipt splitTips(long eventId, long receiptId, long userId) {
        requireDraft(eventId);
        eventAccessPort.requireParticipant(eventId, userId);
        var receipt =
                receiptRepository.findByIdAndEventId(receiptId, eventId).orElseThrow(ReceiptNotFoundException::new);

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
    public void discardPositions(long eventId, long receiptId, long userId) {
        requireDraft(eventId);
        eventAccessPort.requireParticipant(eventId, userId);
        var receipt =
                receiptRepository.findByIdAndEventId(receiptId, eventId).orElseThrow(ReceiptNotFoundException::new);
        long fileId = receipt.getFileId();

        List<Long> positionIds = positionRepository.findByReceiptId(receiptId).stream()
                .filter(position -> eventId == position.getEventId())
                .map(PositionEntity::getId)
                .toList();
        if (!positionIds.isEmpty()) {
            selectionRepository.deleteByPositionIdIn(positionIds);
            sharedTargetRepository.deleteByPositionIdIn(positionIds);
            positionRepository.deleteAllById(positionIds);
            positionRepository.flush();
        }
        receiptRepository.delete(receipt);
        receiptRepository.flush();
        if (receiptRepository.findByFileId(fileId).isEmpty()) {
            fileService.delete(fileId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Receipt> listByEvent(long eventId, long userId) {
        eventAccessPort.requireParticipant(eventId, userId);
        return receiptRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(ReceiptServiceImpl::toDomain)
                .toList();
    }

    private MlReceiptResponse resolveRecognition(String imageSha256, byte[] imageBytes, String mimeType, long userId) {
        var cached = receiptRepository.findFirstByImageSha256AndStatusAndMlRawJsonIsNotNullOrderByIdDesc(
                imageSha256, ReceiptStatus.PROCESSED);
        if (cached.isPresent()) {
            try {
                return objectMapper.readValue(cached.get().getMlRawJson(), MlReceiptResponse.class);
            } catch (Exception ex) {
                // Повреждённый кэш не мешает новому запросу к модели.
            }
        }
        receiptParseAccess.recordModelCall(userId);
        return mlServiceClient.recognize(imageBytes, mimeType);
    }

    private static long toKopecks(double rubles) {
        return BigDecimal.valueOf(rubles)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
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
        return new Receipt(
                entity.getId(),
                entity.getEventId(),
                entity.getFileId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getTotalKopecks(),
                entity.getItemsSumKopecks(),
                Receipt.sumMismatch(entity.getTotalKopecks(), entity.getItemsSumKopecks()));
    }
}
