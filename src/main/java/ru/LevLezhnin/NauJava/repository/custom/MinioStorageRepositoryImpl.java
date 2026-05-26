package ru.LevLezhnin.NauJava.repository.custom;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;
import ru.LevLezhnin.NauJava.exception.file.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link ObjectStorageRepository} на базе MinIO SDK.
 *
 * @author Лев Лежнин
 */
public class MinioStorageRepositoryImpl implements ObjectStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(MinioStorageRepositoryImpl.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageRepositoryImpl(MinioClient minioClient, String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public void uploadWithPath(String path, InputStream inputStream, long size, String contentType) {

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Путь к файлу не может быть пустым");
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream не может быть null");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Размер файла должен быть положительным");
        }

        if (contentType != null) {
            try {
                MimeTypeUtils.parseMimeType(contentType);
            } catch (InvalidMimeTypeException e) {
                logger.warn("Неизвестный MIME-тип: {}", contentType);
                contentType = null;
            }
        }

        contentType = contentType != null && !contentType.isBlank() ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        try {
            logger.debug("Загрузка файла в Minio: {}. Размер: {}. Content-Type: {}", path, size, contentType);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            logger.info("Успешно загружен файл: {}", path);
        } catch (Exception e) {
            logger.error("Ошибка при загрузке файла {}: {}", path, e.getMessage(), e);
            throw FileStorageException.uploadFailed(path, e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn("Не удалось закрыть входной поток после загрузки файла {}: {}", path, e.getMessage());
            }
        }
    }

    @Override
    public InputStream downloadByPath(String path) {
        try {
            logger.debug("Скачивание файла из хранилища: {}", path);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            if (isExceptionCausedByFileNotFound(e)) {
                logger.warn("Файл не найден в хранилище: {}", path);
                throw FileStorageException.fileNotFound(path, e);
            }
            logger.error("Ошибка при скачивании файла {}: {}", path, e.getMessage(), e);
            throw FileStorageException.downloadFailed(path, e);
        }
    }

    @Override
    public boolean fileExistsByPath(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            return true;
        } catch (Exception e) {
            if (isExceptionCausedByFileNotFound(e)) {
                return false;
            }
            logger.error("Ошибка при проверке существования файла {}: {}", path, e.getMessage(), e);
            throw new FileStorageException("Ошибка проверки файла: " + path, e);
        }
    }

    @Override
    public long findFileSizeBytesByPath(String path) {
        try {
            StatObjectResponse response = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            return response.size();
        } catch (Exception e) {
            if (isExceptionCausedByFileNotFound(e)) {
                logger.warn("Попытка получить размер у не существующего в хранилище файла: {}", path);
                throw FileStorageException.fileNotFound(path, e);
            }
            logger.error("Не удалось получить размер файла {}: {}", path, e.getMessage(), e);
            throw new FileStorageException("Ошибка получения метаданных: " + path, e);
        }
    }

    @Override
    public void deleteByPath(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            if (isExceptionCausedByFileNotFound(e)) {
                logger.warn("Попытка удалить не существующий в хранилище файл: {}", path);
                return;
            }
            logger.error("Не удалось удалить файл {}: {}", path, e.getMessage(), e);
            throw FileStorageException.deleteFailed(path, e);
        }
    }

    @Override
    public void deleteAllByPathsInBatch(Iterable<String> paths) {
        if (paths == null) {
            logger.warn("Попытка удаления файлов по null-списку путей");
            return;
        }

        List<DeleteObject> validDeleteObjects = new ArrayList<>();
        for (String path : paths) {
            if (path != null && !path.isBlank()) {
                validDeleteObjects.add(new DeleteObject(path));
            }
        }

        if (validDeleteObjects.isEmpty()) {
            logger.debug("Нет валидных путей для удаления");
            return;
        }

        try {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(validDeleteObjects)
                            .build()
            );

            int errorsCount = 0;
            for (Result<DeleteError> result : results) {
                try {
                    DeleteError error = result.get();

                    if (!isExceptionCausedByFileNotFound(new ErrorResponseException(error, null, null))) {
                        logger.warn("Не удалось удалить объект {}: {} ({})", error.objectName(), error.code(), error.message());
                        ++errorsCount;
                    }
                } catch (Exception e) {
                    logger.error("Ошибка при  обработке результата удаления: {}", e.getMessage(), e);
                    ++errorsCount;
                }
            }

            if (errorsCount == 0) {
                logger.info("Успешно удалено {} файлов из хранилища", validDeleteObjects.size());
            } else {
                logger.warn("Завершено пакетное удаление: {} успешно, {} с ошибками",
                        validDeleteObjects.size() - errorsCount, errorsCount);
            }

        } catch (Exception e) {
            logger.error("Возникла ошибка при пакетном удалении файлов");
            throw FileStorageException.deleteFailed("batch[%d файлов]".formatted(validDeleteObjects.size()), e);
        }
    }

    private boolean isExceptionCausedByFileNotFound(Exception e) {
        if (!(e instanceof ErrorResponseException ex)) {
            return false;
        }
        var errorResponse = ex.errorResponse();
        if (errorResponse == null || errorResponse.code() == null) {
            return false;
        }
        String code = errorResponse.code();
        return "NoSuchKey".equals(code) ||
                "NoSuchBucket".equals(code) ||
                "ResourceNotFound".equals(code);
    }
}
