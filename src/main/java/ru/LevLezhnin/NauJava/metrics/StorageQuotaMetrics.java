package ru.LevLezhnin.NauJava.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Метрики квот хранилища: обновления, превышения, распределение использования.
 */
@Component
public class StorageQuotaMetrics {

    private final Counter quotaUpdatesSuccess;
    private final Counter quotaUpdatesRejected;

    private final DistributionSummary quotaUsagePercent;

    @Autowired
    public StorageQuotaMetrics(MeterRegistry registry) {
        quotaUpdatesSuccess = Counter.builder("storage.quota.update.success")
                .description("Успешные обновления использования квоты")
                .register(registry);

        quotaUpdatesRejected = Counter.builder("storage.quota.update.rejected")
                .description("Отклонённые обновления из-за превышения лимита")
                .register(registry);

        quotaUsagePercent = DistributionSummary.builder("storage.quota.usage_percent")
                .description("Процент использования квоты пользователем после обновления")
                .baseUnit("percent")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
    }

    public void recordQuotaUpdateSuccess(long usedBytes, long maxBytes) {
        quotaUpdatesSuccess.increment();
        if (maxBytes > 0) {
            double percent = Math.min(100.0, (usedBytes * 100.0) / maxBytes);
            quotaUsagePercent.record(percent);
        }
    }

    public void recordQuotaUpdateRejected() {
        quotaUpdatesRejected.increment();
    }
}