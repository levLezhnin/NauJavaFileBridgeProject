package ru.LevLezhnin.NauJava.repository.custom;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.LevLezhnin.NauJava.exceptions.FileStorageException;

import java.io.IOException;
import java.io.InputStream;

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

        contentType = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";

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
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warn("Не удалось закрыть входной поток после загрузки файла {}: {}", path, e.getMessage());
                }
            }
        }
    }

    @Override
    public InputStream downloadByPath(String path) {
        try {
            logger.debug("Скачивание файла из хранилиза: {}", path);
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
