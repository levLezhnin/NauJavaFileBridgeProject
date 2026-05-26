package ru.LevLezhnin.NauJava.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Конфигурация подключения к MinIO (префикс {@code minio.*} в application.yml / .properties).
 * <p>
 * Содержит:
 * <ul>
 *   <li>URL сервера</li>
 *   <li>Access Key / Secret Key</li>
 *   <li>Карту бакетов (на данный момент используется {@link BucketKeys#FILE_BUCKET})</li>
 * </ul>
 * Используется в {@link ru.LevLezhnin.NauJava.config.MinioConfig} и {@link ru.LevLezhnin.NauJava.init.BucketInitializer}.
 *
 * @author Лев Лежнин
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /** URL MinIO-сервера (например http://localhost:9000) */
    private String url;

    /** Access Key для аутентификации в MinIO */
    private String accessKey;

    /** Secret Key для аутентификации в MinIO */
    private String secretKey;

    /** Карта бакетов (ключ - настройки бакета) */
    private Map<String, BucketProperties> buckets;

    private ConnectionProperties connection = new ConnectionProperties();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Map<String, BucketProperties> getBuckets() {
        return Collections.unmodifiableMap(buckets);
    }

    public void setBuckets(Map<String, BucketProperties> buckets) {
        this.buckets = buckets;
    }

    public ConnectionProperties getConnection() {
        return connection;
    }

    public void setConnection(ConnectionProperties connection) {
        this.connection = connection;
    }

    /**
     * Настройки конкретного бакета MinIO.
     */
    public static class BucketProperties {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Ключи для доступа к настроенным бакетам в {@code minio.buckets.*}.
     * <p>
     * В текущей версии проекта используется только {@link #FILE_BUCKET}.
     */
    public enum BucketKeys {
        /** Бакет для хранения пользовательских файлов */
        FILE_BUCKET("fileBucket");

        private final String key;

        public String getKey() {
            return key;
        }

        BucketKeys(String key) {
            this.key = key;
        }
    }

    /**
     * Настройки HTTP-клиента для MinIO
     */
    public static class ConnectionProperties {

        /** Таймаут соединения */
        private Duration connectTimeout = Duration.ofSeconds(3);
        /** Таймаут записи файла в хранилище */
        private Duration writeTimeout = Duration.ofSeconds(30);
        /** Таймаут чтения файла из хранилища */
        private Duration readTimeout = Duration.ofSeconds(30);
        /** Максимальное количество простаивающих соединений */
        private int maxIdleConnections = 20;
        /** Keep-alive */
        private Duration keepAliveDuration = Duration.ofMinutes(5);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(Duration writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getMaxIdleConnections() {
            return maxIdleConnections;
        }

        public void setMaxIdleConnections(int maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
        }

        public Duration getKeepAliveDuration() {
            return keepAliveDuration;
        }

        public void setKeepAliveDuration(Duration keepAliveDuration) {
            this.keepAliveDuration = keepAliveDuration;
        }
    }
}
