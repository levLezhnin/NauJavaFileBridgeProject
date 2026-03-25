package ru.LevLezhnin.NauJava.repository.custom;

import io.minio.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.LevLezhnin.NauJava.properties.MinioProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class MinioFileStorageRepositoryIntegrationTest {

    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";
    private static final String BUCKET = "file-bucket";

    @Container
    static GenericContainer<?> minio =
            new GenericContainer<>("minio/minio:latest")
                    .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
                    .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
                    .withCommand("server /data")
                    .withExposedPorts(9000)
                    .waitingFor(
                            org.testcontainers.containers.wait.strategy.Wait
                                    .forHttp("/minio/health/live")
                                    .forPort(9000)
                    );

    static MinioFileStorageRepositoryImpl repository;

    @BeforeAll
    static void setup() throws Exception {
        String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);

        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(ACCESS_KEY, SECRET_KEY)
                .build();

        // создаём бакет
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(BUCKET).build()
        );

        if (!exists) {
            client.makeBucket(
                    MakeBucketArgs.builder().bucket(BUCKET).build()
            );
        }

        MinioProperties props = new MinioProperties();
        props.setBucket(BUCKET);

        repository = new MinioFileStorageRepositoryImpl(client, props);
    }

    @Test
    void uploadAndDownloadFile() throws Exception {
        String path = "test/file.txt";
        byte[] data = "hello-minio".getBytes();

        repository.uploadWithPath(
                path,
                new ByteArrayInputStream(data),
                data.length,
                "text/plain"
        );

        InputStream downloaded = repository.downloadByPath(path);
        byte[] result = downloaded.readAllBytes();

        assertArrayEquals(data, result);
    }

    @Test
    void fileExists_shouldReturnTrue() {
        String path = "exists/test.txt";
        byte[] data = "data".getBytes();

        repository.uploadWithPath(path, new ByteArrayInputStream(data), data.length, null);

        assertTrue(repository.fileExistsByPath(path));
    }

    @Test
    void findFileSize_shouldReturnCorrectSize() {
        String path = "size/file.bin";
        byte[] data = new byte[1024];

        repository.uploadWithPath(path, new ByteArrayInputStream(data), data.length, null);

        long size = repository.findFileSizeBytesByPath(path);

        assertEquals(1024, size);
    }

    @Test
    void deleteFile_shouldRemoveFile() {
        String path = "delete/file.txt";
        byte[] data = "delete-me".getBytes();

        repository.uploadWithPath(path, new ByteArrayInputStream(data), data.length, null);

        repository.deleteByPath(path);

        assertFalse(repository.fileExistsByPath(path));
    }

    @Test
    void downloadMissingFile_shouldThrowException() {
        assertThrows(Exception.class,
                () -> repository.downloadByPath("missing/file.txt"));
    }
}