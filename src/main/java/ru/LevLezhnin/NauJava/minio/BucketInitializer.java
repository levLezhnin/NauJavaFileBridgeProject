package ru.LevLezhnin.NauJava.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
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
@Profile("dev")
public class BucketInitializer {

    private static final Logger log = LoggerFactory.getLogger(BucketInitializer.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Autowired
    public BucketInitializer(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initBucket() {
        minioProperties.getBuckets().forEach(this::ensureBucketInitialized);
    }

    private void ensureBucketInitialized(String bucketKey, MinioProperties.BucketProperties bucketProperties) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                                                                      .bucket(bucketProperties.getName())
                                                                      .build());

            if (!exists) {
                log.warn("Бакет {} не найден. Создаётся новый бакет", bucketProperties.getName());
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketProperties.getName())
                                .build());
                log.info("Бакет {} создан.", bucketProperties.getName());
            } else {
                log.info("Бакет {} существует.", bucketProperties.getName());
            }

        } catch (Exception e) {
            log.error("Ошибка создания бакета", e);
            throw new FileStorageException("Не удалось создать бакет", e);
        }
    }
}
