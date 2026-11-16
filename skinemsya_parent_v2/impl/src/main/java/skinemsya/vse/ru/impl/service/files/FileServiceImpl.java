package skinemsya.vse.ru.impl.service.files;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinemsya.vse.ru.api.internal.enums.ErrorCode;
import skinemsya.vse.ru.api.internal.enums.files.FileUploadPurpose;
import skinemsya.vse.ru.api.internal.ex.DomainException;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;
import skinemsya.vse.ru.api.internal.repository.files.FileRepository;
import skinemsya.vse.ru.api.internal.service.files.FileService;
import skinemsya.vse.ru.api.internal.service.files.FileSharedAccessVerifier;
import skinemsya.vse.ru.impl.support.properties.FileStorageProperties;

/** Загрузка файлов и проверка доступа к ним. */
@Service
@Transactional
public class FileServiceImpl implements FileService {

    private static final Set<String> IMAGE_MIME_PREFIXES = Set.of("image/");
    private static final String PDF_MIME_TYPE = "application/pdf";

    private final FileRepository fileRepository;
    private final FileStorage fileStorage;
    private final List<FileSharedAccessVerifier> sharedAccessVerifiers;
    private final long maxUploadSizeBytes;

    public FileServiceImpl(
            FileRepository fileRepository,
            FileStorage fileStorage,
            List<FileSharedAccessVerifier> sharedAccessVerifiers,
            FileStorageProperties fileStorageProperties) {
        this.fileRepository = fileRepository;
        this.fileStorage = fileStorage;
        this.sharedAccessVerifiers = sharedAccessVerifiers;
        this.maxUploadSizeBytes = fileStorageProperties.maxUploadSizeBytes();
    }

    @Override
    public StoredFile upload(
            long ownerId,
            String originalName,
            String mimeType,
            long sizeBytes,
            InputStream content,
            FileUploadPurpose purpose) {
        validateFile(content, mimeType, sizeBytes, purpose);
        var storagePath = fileStorage.store(content, extractExtension(originalName));
        return fileRepository.save(StoredFile.builder()
                .ownerId(ownerId)
                .originalName(originalName)
                .mimeType(mimeType)
                .sizeBytes(sizeBytes)
                .storagePath(storagePath)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findById(long fileId) {
        return fileRepository.findById(fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile requireById(long fileId) {
        return findById(fileId).orElseThrow(FileServiceImpl::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getContent(long fileId, long requesterId, boolean allowSharedAccess) {
        var file = requireById(fileId);
        if (file.getOwnerId() != requesterId && !isSharedAccessGranted(fileId, requesterId, allowSharedAccess)) {
            throw denied();
        }
        return fileStorage.read(file.getStoragePath());
    }

    @Override
    public void delete(long fileId) {
        var file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return;
        }
        fileStorage.delete(file.getStoragePath());
        fileRepository.delete(fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOwnerOrShared(long fileId, long requesterId, boolean sharedAccessGranted) {
        var file = requireById(fileId);
        if (file.getOwnerId() != requesterId && !isSharedAccessGranted(fileId, requesterId, sharedAccessGranted)) {
            throw denied();
        }
    }

    private boolean isSharedAccessGranted(long fileId, long requesterId, boolean allowSharedAccess) {
        if (!allowSharedAccess) {
            return false;
        }
        return sharedAccessVerifiers.stream().anyMatch(verifier -> verifier.canAccess(fileId, requesterId));
    }

    private void validateFile(InputStream content, String mimeType, long sizeBytes, FileUploadPurpose purpose) {
        if (content == null || sizeBytes <= 0) {
            throw invalidType(purpose);
        }
        if (sizeBytes > maxUploadSizeBytes) {
            throw tooLarge(maxUploadSizeBytes);
        }
        if (!isAllowedMimeType(mimeType, purpose)) {
            throw invalidType(purpose);
        }
    }

    private static boolean isAllowedMimeType(String mimeType, FileUploadPurpose purpose) {
        if (mimeType == null) {
            return false;
        }
        if (IMAGE_MIME_PREFIXES.stream().anyMatch(mimeType::startsWith)) {
            return true;
        }
        return purpose == FileUploadPurpose.PAYMENT_PROOF && PDF_MIME_TYPE.equalsIgnoreCase(mimeType);
    }

    private static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private static DomainException notFound() {
        return new DomainException(ErrorCode.NOT_FOUND, "File not found");
    }

    private static DomainException denied() {
        return new DomainException(ErrorCode.AUTHORIZATION_ERROR, "File access denied");
    }

    private static DomainException invalidType(FileUploadPurpose purpose) {
        var message = switch (purpose) {
            case PAYMENT_PROOF -> "Only image or PDF files are allowed for payment proof";
            case RECEIPT -> "Only image files are allowed for receipt upload";
        };
        return new DomainException(ErrorCode.VALIDATION_ERROR, message);
    }

    private static DomainException tooLarge(long maxSizeBytes) {
        long megabytes = Math.max(1, Math.round(maxSizeBytes / (1024.0 * 1024.0)));
        return new DomainException(ErrorCode.VALIDATION_ERROR, "File exceeds maximum size of " + megabytes + " MB");
    }
}
