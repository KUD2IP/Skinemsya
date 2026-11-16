package skinemsya.vse.ru.impl.service.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.files.FileUploadPurpose;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;
import skinemsya.vse.ru.api.internal.repository.files.FileRepository;
import skinemsya.vse.ru.api.internal.service.files.FileSharedAccessVerifier;
import skinemsya.vse.ru.impl.support.properties.FileStorageProperties;

/** Проверяет правила загрузки и доступа к файлам. */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileSharedAccessVerifier sharedAccessVerifier;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileServiceImpl(
                fileRepository,
                fileStorage,
                List.of(sharedAccessVerifier),
                new FileStorageProperties("local", "/tmp", 20L * 1024 * 1024, null));
    }

    /** Пустой файл для чека отклоняется. */
    @Test
    void shouldRejectEmptyReceipt() {
        assertThatThrownBy(() -> fileService.upload(
                        1L, "receipt.jpg", "image/jpeg", 0L, new ByteArrayInputStream(new byte[0]), FileUploadPurpose.RECEIPT))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).errorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    /** PDF допускается только как подтверждение оплаты. */
    @Test
    void shouldRejectPdfReceiptAndAllowPaymentProof() {
        var pdf = new ByteArrayInputStream(new byte[] {1, 2, 3});

        assertThatThrownBy(() -> fileService.upload(1L, "bill.pdf", "application/pdf", 3L, pdf, FileUploadPurpose.RECEIPT))
                .isInstanceOf(DomainException.class)
                .hasMessage("Only image files are allowed for receipt upload");

        when(fileStorage.store(pdf, "pdf")).thenReturn("stored.pdf");
        when(fileRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var stored = fileService.upload(1L, "bill.pdf", "application/pdf", 3L, pdf, FileUploadPurpose.PAYMENT_PROOF);

        assertThat(stored.getStoragePath()).isEqualTo("stored.pdf");
        assertThat(stored.getMimeType()).isEqualTo("application/pdf");
    }

    /** Чужой файл без общего доступа недоступен, файл владельца доступен. */
    @Test
    void shouldEnforceOwnerOrSharedAccess() {
        var file = StoredFile.builder().id(5L).ownerId(1L).storagePath("a.jpg").build();
        when(fileRepository.findById(5L)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> fileService.requireOwnerOrShared(5L, 2L, false))
                .isInstanceOf(DomainException.class)
                .hasMessage("File access denied");

        fileService.requireOwnerOrShared(5L, 1L, false);

        when(sharedAccessVerifier.canAccess(5L, 2L)).thenReturn(true);
        fileService.requireOwnerOrShared(5L, 2L, true);
    }
}
