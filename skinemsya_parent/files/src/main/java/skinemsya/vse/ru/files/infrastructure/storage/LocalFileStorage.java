package skinemsya.vse.ru.files.infrastructure.storage;

import skinemsya.vse.ru.files.infrastructure.config.FileStorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class LocalFileStorage implements FileStorage {

    private final Path basePath;

    public LocalFileStorage(FileStorageProperties properties) {
        this.basePath = Path.of(properties.path()).toAbsolutePath().normalize();
    }

    @Override
    public String store(InputStream content, String extension) {
        try {
            Files.createDirectories(basePath);
            var fileName = UUID.randomUUID() + (extension != null && !extension.isBlank() ? "." + extension : "");
            var target = basePath.resolve(fileName);
            Files.copy(content, target);
            return fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file", ex);
        }
    }

    @Override
    public Path resolvePath(String storagePath) {
        return basePath.resolve(storagePath).normalize();
    }

    @Override
    public InputStream read(String storagePath) {
        try {
            return Files.newInputStream(resolvePath(storagePath));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read file", ex);
        }
    }
}
