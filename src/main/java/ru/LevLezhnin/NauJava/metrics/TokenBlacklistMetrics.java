package ru.LevLezhnin.NauJava.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Метрики чёрного списка токенов: добавление, проверка, очистка.
 */
@Component
public class TokenBlacklistMetrics {

    private final Counter tokensBlacklisted;
    private final Counter blacklistChecks;
    private final Counter blacklistHits;
    private final Counter cleanupExecutions;

    private final DistributionSummary tokenTtlSummary;
    private final Gauge blacklistSizeGauge;

    private final AtomicLong currentSize = new AtomicLong(0);

    @Autowired
    public TokenBlacklistMetrics(MeterRegistry registry) {
        tokensBlacklisted = Counter.builder("auth.token.blacklist.added")
                .description("Токены, добавленные в чёрный список")
                .register(registry);

        blacklistChecks = Counter.builder("auth.token.blacklist.checks.total")
                .description("Проверки токенов против чёрного списка")
                .register(registry);

        blacklistHits = Counter.builder("auth.token.blacklist.hits.total")
                .description("Токены, найденные в чёрном списке (положительные проверки)")
                .register(registry);

        cleanupExecutions = Counter.builder("auth.token.blacklist.cleanup.total")
                .description("Запуски задачи очистки чёрного списка")
                .register(registry);

        tokenTtlSummary = DistributionSummary.builder("auth.token.blacklist.ttl_seconds")
                .description("TTL токенов при добавлении в чёрный список")
                .baseUnit("seconds")
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(registry);

        blacklistSizeGauge = Gauge.builder("auth.token.blacklist.size",
                        currentSize, AtomicLong::get)
                .description("Текущее количество токенов в чёрном списке")
                .register(registry);
    }

    public void recordTokenBlacklisted(Duration ttl) {
        tokensBlacklisted.increment();
        tokenTtlSummary.record(ttl.getSeconds());
        currentSize.incrementAndGet();
    }

    public void recordBlacklistCheck(boolean found) {
        blacklistChecks.increment();
        if (found) {
            blacklistHits.increment();
        }
    }

    public void recordCleanup(int removedCount) {
        cleanupExecutions.increment();
        currentSize.addAndGet(-removedCount);
        if (currentSize.get() < 0) {
            currentSize.set(0);
        }
    }

    public long getCurrentSize() {
        return currentSize.get();
    }
}