package ru.LevLezhnin.NauJava.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;
import ru.LevLezhnin.NauJava.validation.file.ContentTypeValid;
import ru.LevLezhnin.NauJava.validation.file.FileNameValid;
import ru.LevLezhnin.NauJava.validation.file.FilePasswordValid;
import ru.LevLezhnin.NauJava.validation.file.FileTTLMinutesValid;

/**
 * Внутренний DTO запроса на загрузку файла (после валидации multipart).
 * <p>
 * Создаётся в {@link ru.LevLezhnin.NauJava.controller.api.FileController#uploadFile(MultipartFile, FileUploadWebRequestPayloadDto)} из
 * {@link FileUploadWebRequestPayloadDto} + метаданных MultipartFile.
 * <p>
 * Используется сервисом {@link ru.LevLezhnin.NauJava.service.interfaces.FileService#uploadFile(FileUploadRequestDto, java.io.InputStream)}.
 *
 * @param fileName     имя файла с расширением (например: report.pdf) (обязательный, непустой, валидируется {@link ru.LevLezhnin.NauJava.validation.file.FileNameValidator})
 * @param contentType  MIME-тип (обязательный, непустой, валидируется {@link ru.LevLezhnin.NauJava.validation.file.ContentTypeValidator})
 * @param fileSize     размер в байтах (обязательный, строго положительный)
 * @param ttlMinutes   время жизни в минутах (обязательный, валидируется {@link ru.LevLezhnin.NauJava.validation.file.FileTTLMinutesValidator})
 * @param maxDownloads максимум скачиваний (обязательный, строго положительный)
 * @param password     пароль (опциональный, валидируется {@link ru.LevLezhnin.NauJava.validation.file.FilePasswordValidator})
 *
 * @author Лев Лежнин
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
