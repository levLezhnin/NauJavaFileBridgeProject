package ru.LevLezhnin.NauJava.minio;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.exceptions.FileStorageException;
import ru.LevLezhnin.NauJava.properties.MinioProperties;

@Component
@Profile("prod")
public class BucketValidator {

    private static final Logger log = LoggerFactory.getLogger(BucketValidator.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Autowired
    public BucketValidator(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateBucket() {
        minioProperties.getBuckets().forEach(this::checkBucketCreated);
    }

    private void checkBucketCreated(String bucketKey, MinioProperties.BucketProperties bucketProperties) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketProperties.getName()).build());

            if (!exists) {
                log.error("Бакет {} не найден. Создайте бакет в файловом хранилище перед запуском приложения", bucketProperties.getName());
                throw new FileStorageException("Бакет не найден: " + bucketProperties.getName(), null);
            } else {
                log.info("Бакет {} существует.", bucketProperties.getName());
            }

        } catch (Exception e) {
            log.error("Ошибка валидации бакета", e);
            throw new FileStorageException("Не удалось провалидировать хранилище", e);
        }
    }
}
