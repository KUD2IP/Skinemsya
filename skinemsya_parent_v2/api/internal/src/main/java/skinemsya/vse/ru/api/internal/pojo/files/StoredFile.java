package skinemsya.vse.ru.api.internal.pojo.files;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Сохранённый файл. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    private long id;
    private long ownerId;
    private String originalName;
    private String mimeType;
    private long sizeBytes;
    private String storagePath;
    private Instant createdAt;
}
