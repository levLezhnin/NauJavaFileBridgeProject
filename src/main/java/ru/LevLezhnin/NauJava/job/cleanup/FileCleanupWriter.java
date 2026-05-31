package ru.LevLezhnin.NauJava.job.cleanup;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

    private final MeterRegistry meterRegistry;

    private final Counter cleanupFilesProcessed;
    private final Counter cleanupFilesFailed;
    private final Counter cleanupQuotasUpdated;
    private final DistributionSummary cleanupFreedBytes;
    private final Timer cleanupBatchDuration;
    private final Timer cleanupStorageDeleteDuration;

    @Autowired
    public FileCleanupWriter(FileRepository fileRepository, FileStatisticsRepository fileStatisticsRepository, UserRepository userRepository, ObjectStorageRepository fileStorageRepository, StorageQuotaService storageQuotaService, MeterRegistry meterRegistry) {
        this.fileRepository = fileRepository;
        this.fileStatisticsRepository = fileStatisticsRepository;
        this.userRepository = userRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.storageQuotaService = storageQuotaService;
        this.meterRegistry = meterRegistry;

        cleanupFilesProcessed = Counter.builder("cleanup.files.processed")
                .description("Количество файлов, обработанных задачей очистки")
                .tag("status", "success")
                .register(meterRegistry);

        cleanupFilesFailed = Counter.builder("cleanup.files.processed")
                .description("Количество файлов, при обработке который произошла ошибка задачей очистки")
                .tag("status", "failed")
                .register(meterRegistry);

        cleanupQuotasUpdated = Counter.builder("cleanup.quotas.updated")
                .description("Количество обновлений квот пользователей")
                .register(meterRegistry);

        cleanupFreedBytes = DistributionSummary.builder("cleanup.freed.bytes")
                .description("Объём освобождённого места при очистке")
                .baseUnit("bytes")
                .publishPercentileHistogram()
                .register(meterRegistry);

        cleanupBatchDuration = Timer.builder("cleanup.batch.duration")
                .description("Время обработки одной пачки файлов (БД + квоты)")
                .publishPercentileHistogram()
                .register(meterRegistry);

        cleanupStorageDeleteDuration = Timer.builder("cleanup.storage.delete.duration")
                .description("Время удаления файлов из объектного хранилища (MinIO)")
                .register(meterRegistry);
    }

    @Override
    public void write(@NotNull Chunk<? extends FileCleanupRecord> chunk) throws Exception {

        Timer.Sample batchDurationTimer = Timer.start(meterRegistry);

        if (chunk.isEmpty()) {
            return;
        }

        log.debug("Начало обработки пачки файлов: size={}", chunk.size());

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
                log.warn("Не найден автор файла с id: {}. Квота не будет обновлена.", authorId);
                continue;
            }

            storageQuotaService.updateStorageQuota(author.getStorageQuota().getId(), -entry.getValue());
            ++quotasMutated;
            bytesFreed += entry.getValue();

            cleanupQuotasUpdated.increment();
            cleanupFreedBytes.record(entry.getValue());
        }

        cleanupFilesProcessed.increment(chunk.size());

        Timer.Sample objectStorageCleanupSample = Timer.start(meterRegistry);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fileStorageRepository.deleteAllByPathsInBatch(deletePaths);
                        objectStorageCleanupSample.stop(cleanupStorageDeleteDuration);
                    } catch (Exception e) {
                        log.error("Не удалось очистить файлы в Minio: {}", deletePaths, e);
                        cleanupFilesFailed.increment(deletePaths.size());
                    }
                }
            });
        } else {
            fileStorageRepository.deleteAllByPathsInBatch(deletePaths);
        }

        log.info("Пачка обработана: удалено файлов={}, обновлено квот={}, освобождено байт={}",
                deleteFileIds.size(), quotasMutated, bytesFreed);

        batchDurationTimer.stop(cleanupBatchDuration);
    }
}
