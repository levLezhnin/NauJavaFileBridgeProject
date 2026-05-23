package ru.LevLezhnin.NauJava.job.file.cleanup;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileStatisticsRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class FileCleanupWriter implements ItemWriter<FileCleanupRecord> {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupWriter.class);

    private final FileRepository fileRepository;
    private final FileStatisticsRepository fileStatisticsRepository;
    private final UserRepository userRepository;
    private final ObjectStorageRepository fileStorageRepository;

    private final StorageQuotaService storageQuotaService;

    @Autowired
    public FileCleanupWriter(FileRepository fileRepository, FileStatisticsRepository fileStatisticsRepository, UserRepository userRepository, ObjectStorageRepository fileStorageRepository, StorageQuotaService storageQuotaService) {
        this.fileRepository = fileRepository;
        this.fileStatisticsRepository = fileStatisticsRepository;
        this.userRepository = userRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.storageQuotaService = storageQuotaService;
    }

    @Override
    public void write(@NotNull Chunk<? extends FileCleanupRecord> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        List<UUID> deleteFileIds = new ArrayList<>();
        List<String> deletePaths = new ArrayList<>();
        Map<Long, Long> freedBytesByAuthor = new HashMap<>();

        chunk.forEach((file) -> {
            deletePaths.add(file.path());
            deleteFileIds.add(file.id());
            freedBytesByAuthor.merge(file.authorId(), file.fileSizeBytes(), Long::sum);
        });

        List<User> authors = userRepository.findAllById(freedBytesByAuthor.keySet());
        Map<Long, User> authorMap = authors.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Long> statisticsIds = fileStatisticsRepository.findStatisticsIdsByFileIds(deleteFileIds);
        fileStatisticsRepository.deleteAllByIdInBatch(statisticsIds);
        fileRepository.deleteAllByIdInBatch(deleteFileIds);

        int quotasMutated = 0;
        long bytesFreed = 0;
        for (Map.Entry<Long, Long> entry : freedBytesByAuthor.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {

            Long authorId = entry.getKey();
            User author = authorMap.get(authorId);

            if (author == null) {
                log.info("Не найден автор файла с id: {}", authorId);
                continue;
            }

            storageQuotaService.updateStorageQuota(author.getStorageQuota().getId(), -entry.getValue());
            ++quotasMutated;
            bytesFreed += entry.getValue();
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorageRepository.deleteAllByPathsInBatch(deletePaths);
                    } catch (Exception e) {
                        log.error("Не удалось очистить файлы в Minio: {}", deletePaths, e);
                    }
                }
            });
        } else {
            fileStorageRepository.deleteAllByPathsInBatch(deletePaths);
        }

        log.info("После очистки файлов были обновлены квоты для {} пользователей. Освобождено {} байт", quotasMutated, bytesFreed);
    }
}
