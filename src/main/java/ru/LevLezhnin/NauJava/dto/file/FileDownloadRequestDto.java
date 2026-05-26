package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO запроса на скачивание файла.
 * <p>
 * Используется в {@code POST /api/v1/files/download}. Передаётся в теле запроса.
 * <p>
 * Если у файла установлен пароль - он должен быть передан в этом DTO.
 *
 * @param fileId   UUID файла (обязателен)
 * @param password пароль (null или пустой, если файл без пароля)
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.api.FileController#downloadFileById(FileDownloadRequestDto)
 * @see ru.LevLezhnin.NauJava.service.interfaces.FileService#downloadById(FileDownloadRequestDto)
 */
public record FileDownloadRequestDto(@JsonProperty("file_id") String fileId, String password) {}
