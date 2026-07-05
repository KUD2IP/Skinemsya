package skinemsya.vse.ru.files.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "skinemsya.file-storage")
public record FileStorageProperties(String type, String path, Long maxUploadSizeBytes, S3 s3) {

    public FileStorageProperties {
        if (type == null || type.isBlank()) {
            type = "local";
        }
        if (maxUploadSizeBytes == null || maxUploadSizeBytes <= 0) {
            maxUploadSizeBytes = 20L * 1024 * 1024;
        }
    }

    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey) {
        public S3 {
            if (region == null || region.isBlank()) {
                region = "us-east-1";
            }
        }
    }
}
