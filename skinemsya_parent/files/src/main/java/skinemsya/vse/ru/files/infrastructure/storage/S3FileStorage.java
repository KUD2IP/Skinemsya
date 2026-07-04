package skinemsya.vse.ru.files.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skinemsya.vse.ru.files.infrastructure.config.FileStorageProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

public class S3FileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(S3FileStorage.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorage(FileStorageProperties properties) {
        var s3 = properties.s3();
        if (s3 == null || s3.endpoint() == null || s3.endpoint().isBlank()) {
            throw new IllegalStateException("S3 endpoint is not configured");
        }
        if (s3.bucket() == null || s3.bucket().isBlank()) {
            throw new IllegalStateException("S3 bucket is not configured");
        }
        if (s3.accessKey() == null || s3.accessKey().isBlank()
                || s3.secretKey() == null || s3.secretKey().isBlank()) {
            throw new IllegalStateException("S3 credentials are not configured");
        }

        this.bucket = s3.bucket().trim();
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3.endpoint().trim()))
                .region(Region.of(s3.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.accessKey().trim(), s3.secretKey().trim())
                ))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        ensureBucketExists();
    }

    void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (Exception ex) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created S3 bucket {}", bucket);
            } catch (Exception createEx) {
                log.warn("Could not ensure S3 bucket {} exists", bucket, createEx);
            }
        }
    }

    @Override
    public String store(InputStream content, String extension) {
        var fileName = UUID.randomUUID() + (extension != null && !extension.isBlank() ? "." + extension : "");
        try {
            byte[] bytes = content.readAllBytes();
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(fileName).build(),
                    RequestBody.fromBytes(bytes)
            );
            return fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file", ex);
        }
    }

    @Override
    public Path resolvePath(String storagePath) {
        throw new UnsupportedOperationException("S3 storage does not expose local paths");
    }

    @Override
    public InputStream read(String storagePath) {
        return s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(storagePath).build());
    }
}
