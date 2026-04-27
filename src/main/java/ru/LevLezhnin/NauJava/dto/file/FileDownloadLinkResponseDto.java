package ru.LevLezhnin.NauJava.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileDownloadLinkResponseDto(@JsonProperty("download_link") String downloadLink) {}
