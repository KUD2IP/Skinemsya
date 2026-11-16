package skinemsya.vse.ru.api.internal.repository.files;

import java.util.Optional;
import skinemsya.vse.ru.api.internal.pojo.files.StoredFile;

/** Порт хранения метаданных файлов. */
public interface FileRepository {

    /** Ищет файл по идентификатору. */
    Optional<StoredFile> findById(long fileId);

    /** Сохраняет метаданные файла. */
    StoredFile save(StoredFile file);

    /** Удаляет метаданные файла. */
    void delete(long fileId);
}
