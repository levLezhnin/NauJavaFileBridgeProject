package ru.LevLezhnin.NauJava.job.cleanup;

import java.time.Instant;
import java.util.UUID;

public record FileCleanupRecord(
        UUID id,
        String path,
        Instant expireAt,
        Long maxDownloads,
        Long timesDownloaded,
        Long authorId,
        Long fileSizeBytes) {}
