package skinemsya.vse.ru.files.api.dto;

import java.time.Instant;

public record FileResponse(long id, String originalName, String mimeType, long sizeBytes, Instant createdAt) {}
