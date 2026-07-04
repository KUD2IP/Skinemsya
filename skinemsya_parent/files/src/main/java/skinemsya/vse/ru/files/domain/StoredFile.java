package skinemsya.vse.ru.files.domain;

import java.time.Instant;

public record StoredFile(
        long id,
        long ownerId,
        String originalName,
        String mimeType,
        long sizeBytes,
        String storagePath,
        Instant createdAt
) {
}
