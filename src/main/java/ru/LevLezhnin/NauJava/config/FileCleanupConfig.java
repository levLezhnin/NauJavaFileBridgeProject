package ru.LevLezhnin.NauJava.config;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import ru.LevLezhnin.NauJava.job.cleanup.FileCleanupProcessor;
import ru.LevLezhnin.NauJava.job.cleanup.FileCleanupReader;
import ru.LevLezhnin.NauJava.job.cleanup.FileCleanupRecord;
import ru.LevLezhnin.NauJava.job.cleanup.FileCleanupWriter;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.repository.jpa.FileStatisticsRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.service.interfaces.StorageQuotaService;

import java.io.IOException;

/**
 * Конфигурация Spring Batch Job для автоматической очистки просроченных файлов.
 * <p>
 * Читает истёкшие файлы, удаляет из объектного хранилища и БД, возвращает квоту.
 *
 * @author Лев Лежнин
 */
@Configuration
public class FileCleanupConfig {

    @Value("${app.files.cleanup.fetch-size:100}")
    private Integer fetchSize;
    @Value("${app.files.cleanup.skip-limit:50}")
    private Integer skipLimit;

    @Bean
    @StepScope
    public FileCleanupReader cleanupReader(FileRepository fileRepository) {
        return new FileCleanupReader(fileRepository, fetchSize);
    }

    @Bean
    public ItemProcessor<FileCleanupRecord, FileCleanupRecord> cleanupProcessor(ObjectStorageRepository fileStorageRepository) {
        return new FileCleanupProcessor(fileStorageRepository);
    }

    @Bean
    public ItemWriter<FileCleanupRecord> cleanupWriter(FileRepository fileRepository,
                                                       FileStatisticsRepository fileStatisticsRepository,
                                                       UserRepository userRepository,
                                                       ObjectStorageRepository fileStorageRepository,
                                                       StorageQuotaService storageQuotaService) {
        return new FileCleanupWriter(fileRepository, fileStatisticsRepository, userRepository, fileStorageRepository, storageQuotaService);
    }

    @Bean
    public Step cleanupStep(ItemReader<FileCleanupRecord> cleanupReader,
                            ItemProcessor<FileCleanupRecord, FileCleanupRecord> cleanupProcessor,
                            ItemWriter<FileCleanupRecord> cleanupWriter,
                            PlatformTransactionManager platformTransactionManager,
                            JobRepository jobRepository) {
        return new StepBuilder("fileCleanupStep", jobRepository)
                .<FileCleanupRecord, FileCleanupRecord>chunk(fetchSize)
                .reader(cleanupReader)
                .processor(cleanupProcessor)
                .writer(cleanupWriter)
                .transactionManager(platformTransactionManager)
                .faultTolerant()
                .skipLimit(skipLimit)
                .skip(IOException.class)
                .build();
    }

    @Bean
    public Job fileCleanupJob(Step cleanupStep, JobRepository jobRepository) {
        return new JobBuilder("fileCleanupJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cleanupStep)
                .build();
    }

}
