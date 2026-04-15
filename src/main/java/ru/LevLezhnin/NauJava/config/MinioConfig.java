package ru.LevLezhnin.NauJava.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.LevLezhnin.NauJava.properties.MinioProperties;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;
import ru.LevLezhnin.NauJava.repository.custom.MinioStorageRepositoryImpl;

@Configuration
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Autowired
    public MinioConfig(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean("fileStorageRepository")
    public ObjectStorageRepository fileStorage(MinioClient minioClient) {
        return new MinioStorageRepositoryImpl(
                minioClient,
                minioProperties.getBuckets()
                        .get(MinioProperties.BucketKeys.FILE_BUCKET.getKey())
                        .getName());
    }

    @Bean("reportStorageRepository")
    public ObjectStorageRepository reportStorage(MinioClient minioClient) {
        return new MinioStorageRepositoryImpl(
                minioClient,
                minioProperties.getBuckets()
                        .get(MinioProperties.BucketKeys.REPORT_BUCKET.getKey())
                        .getName());
    }
}
