package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import ru.LevLezhnin.NauJava.validation.FilePasswordValid;
import ru.LevLezhnin.NauJava.validation.FileTTLMinutesValid;

public record FileUploadWebRequestPayloadDto(
        @FileTTLMinutesValid @JsonProperty("ttl_minutes") Long ttlMinutes,
        @Positive @JsonProperty("max_downloads") Long maxDownloads,
        @FilePasswordValid String password) {}
