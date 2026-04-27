package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO для запроса скачивания файла
 * @param fileId id запрашиваемого файла
 * @param password пароль для доступа к файлу (может быть null)
 */
public record FileDownloadRequestDto(@JsonProperty("file_id") String fileId, String password) {}
