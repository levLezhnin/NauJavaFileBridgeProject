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

    private static final Logger log = LoggerFactory.getLogger(BucketValidator.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Autowired
    public BucketInitializer(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build());

            if (!exists) {
                log.warn("Бакет {} не найден. Создаётся новый бакет", minioProperties.getBucket());
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .build());
                log.info("Бакет {} создан.", minioProperties.getBucket());
            } else {
                log.info("Бакет {} существует.", minioProperties.getBucket());
            }

        } catch (Exception e) {
            log.error("Ошибка создания бакета", e);
            throw new FileStorageException("Не удалось создать бакет", e);
        }
    }
}
