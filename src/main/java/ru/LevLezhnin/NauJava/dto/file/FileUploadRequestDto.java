package ru.LevLezhnin.NauJava.dto.file;

import jakarta.validation.constraints.Positive;
import ru.LevLezhnin.NauJava.validation.ContentTypeValid;
import ru.LevLezhnin.NauJava.validation.FileNameValid;
import ru.LevLezhnin.NauJava.validation.FilePasswordValid;
import ru.LevLezhnin.NauJava.validation.FileTTLMinutesValid;

/**
 * DTO для тела запроса на загрузку файла в систему
 * @param fileName имя файла с расширением (например: report.pdf)
 * @param contentType MIME-тип файла (например: text/html, application/pdf, ...) обязательно должен прийти в заголовке запроса
 * @param fileSize размер файла в байтах
 * @param ttlMinutes время жизни файла в минутах
 * @param maxDownloads ограничение на максимальное кол-во скачиваний ( > 0 )
 * @param password пароль для защиты файла (Опционально. null - если пароль на файл не указан пользователем)
 */
public record FileUploadRequestDto(

        @FileNameValid String fileName,
        @ContentTypeValid String contentType,
        @Positive Long fileSize,
        @FileTTLMinutesValid Long ttlMinutes,
        @Positive Long maxDownloads,
        @FilePasswordValid String password) {}
