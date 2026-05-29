package ru.LevLezhnin.NauJava.repository.custom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.config.MinIOTestContainers;
import ru.LevLezhnin.NauJava.exception.file.FileStorageException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MinIOTestContainers.class)
@DisplayName("Интеграционные тесты MinioStorageRepositoryImpl")
class MinioObjectStorageRepositoryIntegrationTest {

    @Autowired
    @Qualifier("fileStorageRepository")
    private ObjectStorageRepository repository;

    private String uniquePrefix;

    @BeforeEach
    void setUp() {
        uniquePrefix = "test-" + UUID.randomUUID() + "/";
    }

    @Nested
    @DisplayName("uploadWithPath")
    class UploadTests {
        @Test
        @DisplayName("Позитивный: загрузка и скачивание файла")
        void shouldUploadAndDownloadFile() throws Exception {
            String path = uniquePrefix + "file.txt";
            byte[] data = "hello-minio".getBytes();

            repository.uploadWithPath(path, new ByteArrayInputStream(data), data.length, MediaType.TEXT_PLAIN_VALUE);
            InputStream downloaded = repository.downloadByPath(path);
            byte[] result = downloaded.readAllBytes();

            assertThat(result).isEqualTo(data);
        }

        @Test
        @DisplayName("Позитивный: fallback на octet-stream при невалидном MIME-типе")
        void shouldFallbackToOctetStreamForInvalidMimeType() throws Exception {
            String path = uniquePrefix + "fallback.bin";
            byte[] data = "binary-data".getBytes();

            repository.uploadWithPath(path, new ByteArrayInputStream(data), data.length, "invalid/mime/type");

            try (InputStream is = repository.downloadByPath(path)) {
                assertThat(is.readAllBytes()).isEqualTo(data);
            }
        }

        @Test
        @DisplayName("Негативный: выбрасывает IllegalArgumentException при null path")
        void shouldThrowWhenPathIsNull() {
            assertThatThrownBy(() -> repository.uploadWithPath(null, new ByteArrayInputStream(new byte[0]), 1, MediaType.TEXT_PLAIN_VALUE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Путь к файлу не может быть пустым");
        }

        @Test
        @DisplayName("Негативный: выбрасывает IllegalArgumentException при size <= 0")
        void shouldThrowWhenSizeIsInvalid() {
            assertThatThrownBy(() -> repository.uploadWithPath("path", new ByteArrayInputStream(new byte[0]), 0, MediaType.TEXT_PLAIN_VALUE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Размер файла должен быть положительным");
        }
    }

    @Nested
    @DisplayName("downloadByPath & fileExistsByPath")
    class ReadTests {
        @Test
        @DisplayName("Позитивный: fileExistsByPath возвращает true после загрузки")
        void shouldReturnTrueWhenFileExists() {
            String path = uniquePrefix + "exists.txt";
            repository.uploadWithPath(path, new ByteArrayInputStream("data".getBytes()), 4, null);
            assertThat(repository.fileExistsByPath(path)).isTrue();
        }

        @Test
        @DisplayName("Позитивный: fileExistsByPath возвращает false для несуществующего файла")
        void shouldReturnFalseWhenFileNotExists() {
            assertThat(repository.fileExistsByPath(uniquePrefix + "ghost.txt")).isFalse();
        }

        @Test
        @DisplayName("Негативный: downloadByPath выбрасывает FileStorageException для missing file")
        void shouldThrowWhenDownloadingMissingFile() {
            assertThatThrownBy(() -> repository.downloadByPath(uniquePrefix + "missing.txt"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Не найден файл по пути: ");
        }
    }

    @Nested
    @DisplayName("findFileSizeBytesByPath")
    class SizeTests {
        @Test
        @DisplayName("Позитивный: возвращает корректный размер")
        void shouldReturnCorrectSize() {
            String path = uniquePrefix + "size.bin";
            byte[] data = new byte[1024];
            repository.uploadWithPath(path, new ByteArrayInputStream(data), data.length, null);

            assertThat(repository.findFileSizeBytesByPath(path)).isEqualTo(1024L);
        }

        @Test
        @DisplayName("Негативный: выбрасывает FileStorageException для несуществующего файла")
        void shouldThrowWhenGettingSizeOfMissingFile() {
            assertThatThrownBy(() -> repository.findFileSizeBytesByPath(uniquePrefix + "no_size.txt"))
                    .isInstanceOf(FileStorageException.class)
                    .hasMessageContaining("Не найден файл по пути: ");
        }
    }

    @Nested
    @DisplayName("deleteByPath & deleteAllByPathsInBatch")
    class DeleteTests {
        @Test
        @DisplayName("Позитивный: одиночное удаление")
        void shouldDeleteSingleFile() {
            String path = uniquePrefix + "delete_single.txt";
            repository.uploadWithPath(path, new ByteArrayInputStream("delete-me".getBytes()), 9, null);
            repository.deleteByPath(path);

            assertThat(repository.fileExistsByPath(path)).isFalse();
        }

        @Test
        @DisplayName("Позитивный: пакетное удаление успешно удаляет все файлы")
        void shouldDeleteAllByPathsInBatch() {
            String path1 = uniquePrefix + "batch1.txt";
            String path2 = uniquePrefix + "batch2.txt";
            repository.uploadWithPath(path1, new ByteArrayInputStream("1".getBytes()), 1, null);
            repository.uploadWithPath(path2, new ByteArrayInputStream("2".getBytes()), 1, null);

            repository.deleteAllByPathsInBatch(List.of(path1, path2));

            assertThat(repository.fileExistsByPath(path1)).isFalse();
            assertThat(repository.fileExistsByPath(path2)).isFalse();
        }

        @Test
        @DisplayName("Негативный: пакетное удаление игнорирует null/пустой список")
        void shouldIgnoreNullOrEmptyPathsInBatch() {
            repository.deleteAllByPathsInBatch(null);
            repository.deleteAllByPathsInBatch(List.of());
            // Ожидается отсутствие исключений
        }

        @Test
        @DisplayName("Позитивный: удаление несуществующего файла не кидает исключение (idempotent)")
        void shouldNotThrowWhenDeletingNonExistentFile() {
            repository.deleteByPath(uniquePrefix + "already_gone.txt");
            // Идемпотентность: не должно кидать исключение
        }
    }
}