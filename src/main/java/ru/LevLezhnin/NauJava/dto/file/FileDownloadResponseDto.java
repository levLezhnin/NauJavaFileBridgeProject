package ru.LevLezhnin.NauJava.dto.file;

import java.io.InputStream;

public record FileDownloadResponseDto(
        InputStream fileDataInputStream,
        String originalFilename,
        String contentType,
        Long sizeBytes
) {}
