package skinemsya.vse.ru.files.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import skinemsya.vse.ru.files.domain.StoredFile;
import skinemsya.vse.ru.files.domain.exception.FileAccessDeniedException;
import skinemsya.vse.ru.files.domain.exception.FileNotFoundException;
import skinemsya.vse.ru.files.domain.exception.FileTooLargeException;
import skinemsya.vse.ru.files.domain.exception.InvalidFileTypeException;
import skinemsya.vse.ru.files.infrastructure.config.FileStorageProperties;
import skinemsya.vse.ru.files.infrastructure.persistence.FileEntity;
import skinemsya.vse.ru.files.infrastructure.persistence.FileRepository;
import skinemsya.vse.ru.files.infrastructure.storage.FileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            FileStorageProperties fileStorageProperties
    ) {
        this.fileRepository = fileRepository;
        this.fileStorage = fileStorage;
        this.sharedAccessVerifiers = sharedAccessVerifiers;
        this.maxUploadSizeBytes = fileStorageProperties.maxUploadSizeBytes();
    }

    @Override
    public StoredFile upload(long ownerId, MultipartFile file, FileUploadPurpose purpose) {
        validateFile(file, purpose);
        String extension = extractExtension(file.getOriginalFilename());
        String storagePath;
        try {
            storagePath = fileStorage.store(file.getInputStream(), extension);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read uploaded file", ex);
        }

        var entity = new FileEntity();
        entity.setOwnerId(ownerId);
        entity.setOriginalName(file.getOriginalFilename());
        entity.setMimeType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setStoragePath(storagePath);
        entity.setCreatedAt(Instant.now());
        return toDomain(fileRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findById(long fileId) {
        return fileRepository.findById(fileId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile requireById(long fileId) {
        return findById(fileId).orElseThrow(FileNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getContent(long fileId, long requesterId, boolean allowSharedAccess) {
        var file = requireById(fileId);
        if (file.ownerId() != requesterId && !isSharedAccessGranted(fileId, requesterId, allowSharedAccess)) {
            throw new FileAccessDeniedException();
        }
        return fileStorage.read(file.storagePath());
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOwnerOrShared(long fileId, long requesterId, boolean sharedAccessGranted) {
        var file = requireById(fileId);
        if (file.ownerId() != requesterId && !isSharedAccessGranted(fileId, requesterId, sharedAccessGranted)) {
            throw new FileAccessDeniedException();
        }
    }

    private boolean isSharedAccessGranted(long fileId, long requesterId, boolean allowSharedAccess) {
        if (!allowSharedAccess) {
            return false;
        }
        return sharedAccessVerifiers.stream().anyMatch(verifier -> verifier.canAccess(fileId, requesterId));
    }

    private void validateFile(MultipartFile file, FileUploadPurpose purpose) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException(purpose);
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new FileTooLargeException(maxUploadSizeBytes);
        }
        var mimeType = file.getContentType();
        if (!isAllowedMimeType(mimeType, purpose)) {
            throw new InvalidFileTypeException(purpose);
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

    private StoredFile toDomain(FileEntity entity) {
        return new StoredFile(
                entity.getId(),
                entity.getOwnerId(),
                entity.getOriginalName(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getStoragePath(),
                entity.getCreatedAt()
        );
    }
}
