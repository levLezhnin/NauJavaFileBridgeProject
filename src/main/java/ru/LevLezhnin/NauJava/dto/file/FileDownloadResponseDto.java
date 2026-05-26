package ru.LevLezhnin.NauJava.dto.file;

import java.io.InputStream;

/**
 * DTO ответа на запрос скачивания файла
 * @param fileDataInputStream   {@link InputStream} с данными файла (обязательный, открытый для чтения)
 * @param originalFilename      оригинальное имя файла (обязательный, непустой)
 * @param contentType           MIME-тип файла (обязательный, непустой)
 * @param sizeBytes             размер файла в байтах (обязательный, положительный)
 * @author Лев Лежнин
 */
public record FileDownloadResponseDto(
        InputStream fileDataInputStream,
        String originalFilename,
        String contentType,
        Long sizeBytes
) {}
