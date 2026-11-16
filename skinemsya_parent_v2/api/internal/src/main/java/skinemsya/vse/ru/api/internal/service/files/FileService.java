package skinemsya.vse.ru.api.internal.service.files;

import java.io.InputStream;
import java.util.Optional;
import skinemsya.vse.ru.api.internal.enums.files.FileUploadPurpose;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;

/** Загрузка файлов и проверка доступа к ним. */
public interface FileService {

    /** Сохраняет файл и его метаданные. */
    StoredFile upload(
            long ownerId,
            String originalName,
            String mimeType,
            long sizeBytes,
            InputStream content,
            FileUploadPurpose purpose);

    /** Ищет файл по идентификатору. */
    Optional<StoredFile> findById(long fileId);

    /** Возвращает файл или ошибку «не найден». */
    StoredFile requireById(long fileId);

    /** Открывает содержимое файла, если у пользователя есть доступ. */
    InputStream getContent(long fileId, long requesterId, boolean allowSharedAccess);

    /** Требует, чтобы пользователь был владельцем файла или имел общий доступ. */
    void requireOwnerOrShared(long fileId, long requesterId, boolean sharedAccessGranted);

    /** Удаляет файл из хранилища и метаданные. */
    void delete(long fileId);
}
