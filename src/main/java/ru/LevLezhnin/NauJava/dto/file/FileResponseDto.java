package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record FileResponseDto(
        String id,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("upload_date") Instant uploadDate,
        @JsonProperty("expire_date") Instant expireDate,
        @JsonProperty("times_downloaded") Long timesDownloaded,
        @JsonProperty("max_downloads") Long maxDownloads,
        @JsonProperty("has_password") Boolean hasPassword
) {}
