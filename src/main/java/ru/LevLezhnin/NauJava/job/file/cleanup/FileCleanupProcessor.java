package ru.LevLezhnin.NauJava.job.file.cleanup;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;

import java.time.Instant;

@Component
public class FileCleanupProcessor implements ItemProcessor<FileCleanupRecord, FileCleanupRecord> {

    private final ObjectStorageRepository fileStorageRepository;

    @Autowired
    public FileCleanupProcessor(ObjectStorageRepository fileStorageRepository) {
        this.fileStorageRepository = fileStorageRepository;
    }

    private boolean isFileExpired(FileCleanupRecord file) {
        if (file.expireAt().isBefore(Instant.now())) {
            return true;
        }
        if (file.maxDownloads() <= file.timesDownloaded()) {
            return true;
        }
        return !fileStorageRepository.fileExistsByPath(file.path());
    }

    @Override
    public @Nullable FileCleanupRecord process(@NotNull FileCleanupRecord file) throws Exception {
        return isFileExpired(file) ? file : null;
    }
}
