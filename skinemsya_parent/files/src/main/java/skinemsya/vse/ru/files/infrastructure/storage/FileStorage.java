package skinemsya.vse.ru.files.infrastructure.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorage {

    String store(InputStream content, String extension);

    Path resolvePath(String storagePath);

    InputStream read(String storagePath);
}
