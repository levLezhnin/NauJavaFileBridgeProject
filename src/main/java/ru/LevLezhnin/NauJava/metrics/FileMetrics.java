package ru.LevLezhnin.NauJava.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.exception.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exception.common.InvalidSearchCriteriaException;
import ru.LevLezhnin.NauJava.exception.file.*;
import ru.LevLezhnin.NauJava.exception.storagequotas.StorageQuotaExceededException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Метрики операций с файлами: загрузка, скачивание, удаление, ошибки, хранилище.
 */
@Component
public class FileMetrics {
    private final Counter fileUploadCounter;
    private final Counter fileDownloadCounter;
    private final Counter fileDeleteCounter;

    private final Meter.MeterProvider<Counter> operationErrors;
    private final Counter quotaRejections;
    private final Counter downloadLimitHits;
    private final Counter expiredFileAttempts;
    private final Counter passwordCheckFailures;

    private final DistributionSummary fileSizeSummary;
    private final Timer fileUploadTimer;
    private final Timer fileDownloadTimer;
    private final Timer fileDeleteTimer;
    private final AtomicLong totalStorageBytes = new AtomicLong();
    private final Gauge totalStorageBytesGauge;

    private final Map<Class<? extends Throwable>, String> errorClassToErrorTypeMap;

    @Autowired
    public FileMetrics(MeterRegistry registry) {

        fileUploadCounter = Counter.builder("file.uploads.total")
                .description("Общее количество загрузок файлов")
                .register(registry);

        fileDownloadCounter = Counter.builder("file.downloads.total")
                .description("Общее количество скачиваний файлов")
                .register(registry);

        fileDeleteCounter = Counter.builder("file.deletions.total")
                .description("Общее количество удалений файлов")
                .register(registry);

        operationErrors = Counter.builder("file.operation.errors")
                .description("Ошибки операций с файлами")
                .withRegistry(registry);

        quotaRejections = Counter.builder("file.rejections.quota_exceeded")
                .description("Попытки загрузки при превышении квоты")
                .register(registry);

        downloadLimitHits = Counter.builder("file.rejections.download_limit")
                .description("Попытки скачивания при исчерпанном лимите")
                .register(registry);

        expiredFileAttempts = Counter.builder("file.rejections.expired")
                .description("Попытки доступа к просроченному файлу")
                .register(registry);

        passwordCheckFailures = Counter.builder("file.rejections.password_check_failure")
                .description("Неверный пароль для защищённого файла")
                .register(registry);

        fileSizeSummary = DistributionSummary.builder("file.size.bytes")
                .description("Распределение размеров файлов")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        fileUploadTimer = Timer.builder("file.upload.duration")
                .description("Время успешной загрузки файла")
                .publishPercentileHistogram()
                .register(registry);

        fileDownloadTimer = Timer.builder("file.download.duration")
                .description("Время успешного скачивания файла")
                .publishPercentileHistogram()
                .register(registry);

        fileDeleteTimer = Timer.builder("file.delete.duration")
                .description("Время успешного удаления файла (метаданные + объектное хранилище)")
                .publishPercentileHistogram()
                .register(registry);

        errorClassToErrorTypeMap = Map.of(
                StorageQuotaExceededException.class, "quota_exceeded",
                DownloadLimitExceededException.class, "download_limit",
                FileExpiredException.class, "expired",
                InvalidPasswordException.class, "password_check_failed",
                FileNotFoundException.class, "not_found",
                FileStorageException.class, "not_found",
                IllegalFileAccessException.class, "access_denied",
                InvalidSearchCriteriaException.class, "bad_request"
        );
        totalStorageBytesGauge = Gauge.builder("file.storage.total_bytes", totalStorageBytes, AtomicLong::get)
                .description("Суммарный объём всех файлов в байтах")
                .baseUnit("bytes")
                .register(registry);
    }

    public void recordUploadSuccess(long sizeBytes, Timer.Sample fileUploadSample) {
        fileUploadCounter.increment();
        fileSizeSummary.record(sizeBytes);
        fileUploadSample.stop(fileUploadTimer);
        totalStorageBytes.addAndGet(sizeBytes);
    }

    public void recordDownloadSuccess(Timer.Sample fileDownloadSample) {
        fileDownloadCounter.increment();
        fileDownloadSample.stop(fileDownloadTimer);
    }

    public void recordDeleteSuccess(long sizeBytes, Timer.Sample fileDeleteSample) {
        fileDeleteCounter.increment();
        fileDeleteSample.stop(fileDeleteTimer);
        totalStorageBytes.addAndGet(-sizeBytes);
    }

    private String mapExceptionToErrorType(Throwable e) {
        if (e == null) {
            return "unknown";
        }

        return errorClassToErrorTypeMap.getOrDefault(e.getClass(), "internal_error");
    }

    public void recordOperationError(String operation, Throwable error) {
        operationErrors.withTags(Tags.of("operation", operation, "error_type", mapExceptionToErrorType(error))).increment();
    }

    public void recordStorageQuotaRejection() {
        quotaRejections.increment();
    }

    public void recordDownloadLimitHit() {
        downloadLimitHits.increment();
    }

    public void recordExpiredFileAttempt() {
        expiredFileAttempts.increment();
    }

    public void recordPasswordCheckFailure() {
        passwordCheckFailures.increment();
    }
}