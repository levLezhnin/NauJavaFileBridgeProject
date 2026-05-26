package ru.LevLezhnin.NauJava.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Конфигурация планировщика задач Spring (для очистки файлов).
 *
 * @author Лев Лежнин
 */
@Configuration
public class SchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    @Bean("fileCleanupSchedulerThreadPool")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("FileCleanupJob-");
        scheduler.initialize();

        log.info("Создан ThreadPoolTaskScheduler. Размер пула: {}, Префикс потоков: {}",
                scheduler.getPoolSize(), scheduler.getThreadNamePrefix());

        return scheduler;
    }

}
