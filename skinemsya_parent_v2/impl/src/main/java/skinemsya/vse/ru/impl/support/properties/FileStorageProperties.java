package skinemsya.vse.ru.impl.support.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Настройки локального и S3-хранилища файлов. */
@ConfigurationProperties(prefix = "skinemsya.file-storage")
public record FileStorageProperties(String type, String path, Long maxUploadSizeBytes, S3 s3) {

    /** Подставляет локальное хранилище и лимит 20 МБ, если значения не заданы. */
    public FileStorageProperties {
        if (type == null || type.isBlank()) {
            type = "local";
        }
        if (maxUploadSizeBytes == null || maxUploadSizeBytes <= 0) {
            maxUploadSizeBytes = 20L * 1024 * 1024;
        }
    }

    /** Параметры бакета S3. */
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey) {

        /** Подставляет регион us-east-1, если он не задан. */
        public S3 {
            if (region == null || region.isBlank()) {
                region = "us-east-1";
            }
        }
    }
}
