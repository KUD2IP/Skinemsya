package skinemsya.vse.ru.impl.service.files;

import java.io.InputStream;
import java.nio.file.Path;

/** Хранилище содержимого файлов. */
public interface FileStorage {

    /** Сохраняет содержимое и возвращает ключ в хранилище. */
    String store(InputStream content, String extension);

    /** Возвращает локальный путь. Для S3 не поддерживается. */
    Path resolvePath(String storagePath);

    /** Открывает содержимое по ключу. */
    InputStream read(String storagePath);

    /** Удаляет содержимое по ключу. */
    void delete(String storagePath);
}
