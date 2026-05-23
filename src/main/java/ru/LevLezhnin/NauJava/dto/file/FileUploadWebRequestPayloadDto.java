package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.LevLezhnin.NauJava.validation.file.FilePasswordValid;
import ru.LevLezhnin.NauJava.validation.file.FileTTLMinutesValid;

public record FileUploadWebRequestPayloadDto(

        @NotNull(message = "Время жизни файла не может быть null")
        @FileTTLMinutesValid
        @JsonProperty("ttl_minutes")
        Long ttlMinutes,

        @NotNull(message = "Максимальное количество скачиваний файла не может быть null")
        @Positive
        @JsonProperty("max_downloads")
        Long maxDownloads,

        @FilePasswordValid
        String password) {}
