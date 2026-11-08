package skinemsya.vse.ru.receipts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.events.application.EventAccessPort;
import skinemsya.vse.ru.events.domain.EventStatus;
import skinemsya.vse.ru.files.application.FileService;
import skinemsya.vse.ru.files.domain.StoredFile;
import skinemsya.vse.ru.integrations.application.MlServiceClient;
import skinemsya.vse.ru.integrations.domain.MlReceiptItem;
import skinemsya.vse.ru.integrations.domain.MlReceiptResponse;
import skinemsya.vse.ru.receipts.domain.ReceiptStatus;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.PositionSelectionRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.ReceiptEntity;
import skinemsya.vse.ru.receipts.infrastructure.persistence.ReceiptRepository;
import skinemsya.vse.ru.receipts.infrastructure.persistence.SharedPositionTargetRepository;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionSelectionRepository selectionRepository;

    @Mock
    private SharedPositionTargetRepository sharedTargetRepository;

    @Mock
    private FileService fileService;

    @Mock
    private MlServiceClient mlServiceClient;

    @Mock
    private EventAccessPort eventAccessPort;

    @Mock
    private ReceiptParseAccess receiptParseAccess;

    private ReceiptServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReceiptServiceImpl(
                receiptRepository,
                positionRepository,
                selectionRepository,
                sharedTargetRepository,
                fileService,
                mlServiceClient,
                eventAccessPort,
                receiptParseAccess,
                new ObjectMapper());
        when(eventAccessPort.getStatus(1L)).thenReturn(EventStatus.DRAFT);
        when(fileService.requireById(5L))
                .thenReturn(new StoredFile(5L, 7L, "receipt.jpg", "image/jpeg", 3, "path", Instant.now()));
        when(fileService.getContent(5L, 7L, false)).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        when(receiptRepository.save(any())).thenAnswer(invocation -> {
            ReceiptEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(10L);
            }
            return entity;
        });
    }

    @Test
    void keepsLowConfidenceLineInsteadOfFailingTheReceipt() {
        when(receiptRepository.findFirstByImageSha256AndStatusAndMlRawJsonIsNotNullOrderByIdDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(mlServiceClient.recognize(any(), any()))
                .thenReturn(new MlReceiptResponse(
                        "Кафе",
                        null,
                        100.0,
                        "RUB",
                        List.of(new MlReceiptItem("Суп", 1, 100.0, 100.0, 0.4)),
                        0.4,
                        List.of()));

        var receipt = service.processReceipt(1L, 5L, 7L);

        assertThat(receipt.status()).isEqualTo(ReceiptStatus.PROCESSED);
        assertThat(receipt.sumMismatch()).isFalse();
        var position = org.mockito.ArgumentCaptor.forClass(PositionEntity.class);
        verify(positionRepository).save(position.capture());
        assertThat(position.getValue().isLowConfidence()).isTrue();
        verify(receiptParseAccess).recordModelCall(7L);
    }

    @Test
    void reusesCachedRecognitionWithoutCallingTheModel() throws Exception {
        var cached = new ReceiptEntity();
        cached.setId(3L);
        cached.setMlRawJson(new ObjectMapper()
                .writeValueAsString(new MlReceiptResponse(
                        null,
                        null,
                        50.0,
                        "RUB",
                        List.of(new MlReceiptItem("Чай", 1, 50.0, 50.0, 0.99)),
                        0.99,
                        List.of())));
        when(receiptRepository.findFirstByImageSha256AndStatusAndMlRawJsonIsNotNullOrderByIdDesc(any(), any()))
                .thenReturn(Optional.of(cached));

        var receipt = service.processReceipt(1L, 5L, 7L);

        assertThat(receipt.status()).isEqualTo(ReceiptStatus.PROCESSED);
        verify(mlServiceClient, never()).recognize(any(), any());
        verify(receiptParseAccess, never()).recordModelCall(any(Long.class));
    }
}
