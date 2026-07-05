package skinemsya.vse.ru.files.application;

import java.io.InputStream;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import skinemsya.vse.ru.files.domain.StoredFile;

public interface FileService {

    StoredFile upload(long ownerId, MultipartFile file, FileUploadPurpose purpose);

    Optional<StoredFile> findById(long fileId);

    StoredFile requireById(long fileId);

    InputStream getContent(long fileId, long requesterId, boolean allowSharedAccess);

    void requireOwnerOrShared(long fileId, long requesterId, boolean sharedAccessGranted);
}
