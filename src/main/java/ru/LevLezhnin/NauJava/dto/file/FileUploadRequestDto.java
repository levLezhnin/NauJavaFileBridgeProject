package ru.LevLezhnin.NauJava.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.LevLezhnin.NauJava.validation.file.ContentTypeValid;
import ru.LevLezhnin.NauJava.validation.file.FileNameValid;
import ru.LevLezhnin.NauJava.validation.file.FilePasswordValid;
import ru.LevLezhnin.NauJava.validation.file.FileTTLMinutesValid;

/**
 * DTO для тела запроса на загрузку файла в систему
 * @param fileName имя файла с расширением (например: report.pdf)
 * @param contentType MIME-тип файла (например: text/html, application/pdf, ...) обязательно должен прийти в заголовке запроса
 * @param fileSize размер файла в байтах
 * @param ttlMinutes время жизни файла в минутах
 * @param maxDownloads ограничение на максимальное кол-во скачиваний ( > 0 )
 * @param password пароль для защиты файла (Опционально. null | blank - если пароль на файл не указан пользователем)
 */
public record FileUploadRequestDto(

        @NotBlank(message = "Имя файла не может быть непустым")
        @FileNameValid
        String fileName,

        @NotBlank(message = "Content-Type не может быть пустым")
        @ContentTypeValid
        String contentType,

        @NotNull(message = "Размер файла не может быть null")
        @Positive
        Long fileSize,

        @NotNull(message = "Время жизни файла не может быть null")
        @FileTTLMinutesValid
        Long ttlMinutes,

        @NotNull(message = "Максимальное количество скачиваний файла не может быть null")
        @Positive
        Long maxDownloads,

        @FilePasswordValid
        String password) {}
