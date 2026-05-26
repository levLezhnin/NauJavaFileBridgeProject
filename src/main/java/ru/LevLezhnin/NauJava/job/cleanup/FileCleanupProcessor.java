package ru.LevLezhnin.NauJava.job.cleanup;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;

import java.time.Instant;

@Component
public class FileCleanupProcessor implements ItemProcessor<FileCleanupRecord, FileCleanupRecord> {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupProcessor.class);

    private final ObjectStorageRepository fileStorageRepository;

    @Autowired
    public FileCleanupProcessor(ObjectStorageRepository fileStorageRepository) {
        this.fileStorageRepository = fileStorageRepository;
    }

    private boolean isFileExpired(FileCleanupRecord file) {
        if (file.expireAt().isBefore(Instant.now())) {
            log.debug("Файл истёк по времени: fileId={}, expireAt={}", file.id(), file.expireAt());
            return true;
        }
        if (file.maxDownloads() <= file.timesDownloaded()) {
            log.debug("Файл исчерпал лимит скачиваний: fileId={}, limit={}", file.id(), file.maxDownloads());
            return true;
        }
        if (!fileStorageRepository.fileExistsByPath(file.path())) {
            log.debug("Файл отсутствует в хранилище: fileId={}, limit={}", file.id(), file.maxDownloads());
            return true;
        }
        return false;
    }

    @Override
    public @Nullable FileCleanupRecord process(@NotNull FileCleanupRecord file) throws Exception {
        log.debug("Обработка записи: fileId={}, path={}, expireAt={}, downloaded={}/{}",
                file.id(), file.path(), file.expireAt(), file.timesDownloaded(), file.maxDownloads());

        boolean expired = isFileExpired(file);

        if (expired) {
            log.debug("Файл помечен на удаление: fileId={}, path={}", file.id(), file.path());
        }

        return expired ? file : null;
    }
}
