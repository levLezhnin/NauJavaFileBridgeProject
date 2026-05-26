package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO ответа с метаданными файла пользователя.
 *
 * @param id                UUID файла
 * @param fileName          оригинальное имя файла
 * @param fileSizeBytes     размер файла в байтах
 * @param uploadDate        дата и время загрузки файла
 * @param expireDate        дата и время истечения файла
 * @param timesDownloaded   количество скачиваний файла
 * @param maxDownloads      максимум скачиваний файла
 * @param hasPassword       защищён ли файл паролем
 *
 * @author Лев Лежнин
 */
public record FileResponseDto(
        String id,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_size_bytes") Long fileSizeBytes,
        @JsonProperty("upload_date") Instant uploadDate,
        @JsonProperty("expire_date") Instant expireDate,
        @JsonProperty("times_downloaded") Long timesDownloaded,
        @JsonProperty("max_downloads") Long maxDownloads,
        @JsonProperty("has_password") Boolean hasPassword
) {}
