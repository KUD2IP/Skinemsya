package skinemsya.vse.ru.files.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import skinemsya.vse.ru.files.infrastructure.storage.FileStorage;
import skinemsya.vse.ru.files.infrastructure.storage.LocalFileStorage;
import skinemsya.vse.ru.files.infrastructure.storage.S3FileStorage;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FilesConfiguration {

    @Bean
    @ConditionalOnProperty(name = "skinemsya.file-storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "skinemsya.file-storage.type", havingValue = "s3")
    public FileStorage s3FileStorage(FileStorageProperties properties) {
        return new S3FileStorage(properties);
    }
}
