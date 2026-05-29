package ru.LevLezhnin.NauJava.config;

import io.minio.*;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;

public class MinIOTestContainers implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    private static MinioClient minioClient;
    private static final String ACCESS_KEY = "test-access-key";
    private static final String SECRET_KEY = "test-secret-key";
    private static final String BUCKET = "test-bucket";

    private static final MinIOContainer minio = new MinIOContainer("minio/minio")
            .withUserName(ACCESS_KEY)
            .withPassword(SECRET_KEY)
            .withReuse(true);

    @Override
    public void beforeAll(ExtensionContext context) {
        minio.start();

        String minioUrl = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);

        System.setProperty("minio.url", minioUrl);
        System.setProperty("minio.accessKey", minio.getUserName());
        System.setProperty("minio.secretKey", minio.getPassword());
        System.setProperty("minio.buckets.fileBucket.name", BUCKET);

        minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minio.getUserName(), minio.getPassword())
                .build();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        createBucketIfNotExists();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        cleanupBucket();
    }

    private void createBucketIfNotExists() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать тестовый бакет", e);
        }
    }

    private void cleanupBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(BUCKET).build());
            if (exists) {
                var objects = minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(BUCKET).recursive(true).build());
                for (var result : objects) {
                    String objectName = result.get().objectName();
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectName)
                            .build());
                }
                minioClient.removeBucket(RemoveBucketArgs.builder().bucket(BUCKET).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Не удалось очистить тестовый бакет", e);
        }
    }

    @DynamicPropertySource
    public static void registerMinioProperties(DynamicPropertyRegistry registry) {
        if (minio.isRunning()) {
            registry.add("minio.url", minio::getS3URL);
            registry.add("minio.accessKey", minio::getUserName);
            registry.add("minio.secretKey", minio::getPassword);

            registry.add("minio.buckets.fileBucket.name", () -> BUCKET);

            registry.add("minio.connection.connectTimeout", () -> "10s");
            registry.add("minio.connection.readTimeout", () -> "30s");
            registry.add("minio.connection.writeTimeout", () -> "30s");
            registry.add("minio.connection.maxIdleConnections", () -> "5");
            registry.add("minio.connection.keepAliveDuration", () -> "5m");
        }
    }

    public static MinioClient getMinioClient() {
        return minioClient;
    }

    public static String getEndpoint() {
        return minio.isRunning() ? minio.getS3URL() : null;
    }

    public static String getAccessKey() {
        return minio.getUserName();
    }

    public static String getSecretKey() {
        return minio.getPassword();
    }
}
