package ru.LevLezhnin.NauJava.config;

import io.minio.MinioClient;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.LevLezhnin.NauJava.config.properties.MinioProperties;
import ru.LevLezhnin.NauJava.repository.custom.MinioStorageRepositoryImpl;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;

import java.util.concurrent.TimeUnit;

/**
 * Конфигурация клиента MinIO для объектного хранилища файлов.
 *
 * @author Лев Лежнин
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    private final MinioProperties minioProperties;

    @Autowired
    public MinioConfig(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @Bean
    public MinioClient minioClient() {

        MinioProperties.ConnectionProperties connectionProperties = minioProperties.getConnection();

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectionProperties.getConnectTimeout())
                .readTimeout(connectionProperties.getReadTimeout())
                .writeTimeout(connectionProperties.getWriteTimeout())
                .connectionPool(new ConnectionPool(
                        connectionProperties.getMaxIdleConnections(),
                        connectionProperties.getKeepAliveDuration().toMillis(),
                        TimeUnit.MILLISECONDS
                ))
                .build();

        log.info("""
                 Создаётся HTTP-клиент для MinIO:
                 Таймаут подключения: {}
                 Таймаут чтения: {}
                 Таймаут записи: {}
                 Максимальное количество простаивающих соединений: {}
                 Keep-alive: {}
                 """,
                connectionProperties.getConnectTimeout(),
                connectionProperties.getReadTimeout(),
                connectionProperties.getWriteTimeout(),
                connectionProperties.getMaxIdleConnections(),
                connectionProperties.getKeepAliveDuration()
        );

        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .httpClient(httpClient)
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
}
