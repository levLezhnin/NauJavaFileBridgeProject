package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO ответа с подробными метаданными файла для администраторов.
 * @param id                UUID файла (обязательный, непустой)
 * @param fileName          оригинальное имя файла (обязательный, непустой)
 * @param mimeType          MIME-тип файла (обязательный, непустой)
 * @param authorId          ID пользователя-автора файла (обязательный, может быть пустым, если пользователя нет в БД)
 * @param authorUsername    логин пользователя-автора файла (обязательный. Если пользователя-автора нет в БД должен быть заполнен как 'Неизвестно')
 * @param fileSizeBytes     размер файла в байтах (обязательный)
 * @param uploadDate        дата и время загрузки файла (обязательный)
 * @param expireDate        дата и время истечения файла (обязательный)
 * @param timesDownloaded   количество скачиваний файла (обязательный)
 * @param maxDownloads      максимум скачиваний файла (обязательный)
 * @param hasPassword       защищён ли файл паролем (обязательный)
 * @author Лев Лежнин
 */
public record FileAdminResponseDto(
        String id,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("author_id") String authorId,
        @JsonProperty("author_username") String authorUsername,
        @JsonProperty("file_size_bytes") Long fileSizeBytes,
        @JsonProperty("upload_date") Instant uploadDate,
        @JsonProperty("expire_date") Instant expireDate,
        @JsonProperty("times_downloaded") Long timesDownloaded,
        @JsonProperty("max_downloads") Long maxDownloads,
        @JsonProperty("has_password") Boolean hasPassword
) {}
