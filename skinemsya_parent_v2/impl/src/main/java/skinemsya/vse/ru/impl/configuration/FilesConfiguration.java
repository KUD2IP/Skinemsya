package skinemsya.vse.ru.impl.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import skinemsya.vse.ru.impl.service.files.FileStorage;
import skinemsya.vse.ru.impl.service.files.LocalFileStorage;
import skinemsya.vse.ru.impl.service.files.S3FileStorage;
import skinemsya.vse.ru.impl.support.properties.FileStorageProperties;

/** Выбирает локальное или S3-хранилище файлов. */
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FilesConfiguration {

    /** Локальный каталог, если тип хранилища не задан или равен local. */
    @Bean
    @ConditionalOnProperty(name = "skinemsya.file-storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorage localFileStorage(FileStorageProperties properties) {
        return new LocalFileStorage(properties);
    }

    /** S3-бакет, если тип хранилища равен s3. */
    @Bean
    @ConditionalOnProperty(name = "skinemsya.file-storage.type", havingValue = "s3")
    public FileStorage s3FileStorage(FileStorageProperties properties) {
        return new S3FileStorage(properties);
    }
}
