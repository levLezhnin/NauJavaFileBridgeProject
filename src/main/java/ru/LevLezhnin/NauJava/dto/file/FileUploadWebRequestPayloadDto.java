package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.LevLezhnin.NauJava.validation.file.FilePasswordValid;
import ru.LevLezhnin.NauJava.validation.file.FileTTLMinutesValid;

/**
 * DTO метаданных файла при загрузке (multipart/form-data, часть "payload").
 * <p>
 * Используется в {@code POST /api/v1/files} вместе с частью "file".
 * Преобразуется в {@link FileUploadRequestDto} внутри контроллера.
 *
 * @param ttlMinutes   время жизни файла в минутах (обязательный, валидируется {@link ru.LevLezhnin.NauJava.validation.file.FileTTLMinutesValidator})
 * @param maxDownloads максимальное количество скачиваний (обязательный, строго положительный)
 * @param password     пароль для доступа к файлу (если null или пустой, то файл не защищён паролем)
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.api.FileController#uploadFile(org.springframework.web.multipart.MultipartFile, FileUploadWebRequestPayloadDto)
 */
public record FileUploadWebRequestPayloadDto(

        @NotNull(message = "Время жизни файла не может быть null")
        @FileTTLMinutesValid
        @JsonProperty("ttl_minutes")
        Long ttlMinutes,

        @NotNull(message = "Максимальное количество скачиваний файла не может быть null")
        @Positive(message = "Максимальное количество скачивание файла должно быть больше 0")
        @JsonProperty("max_downloads")
        Long maxDownloads,

        @FilePasswordValid
        String password) {}
