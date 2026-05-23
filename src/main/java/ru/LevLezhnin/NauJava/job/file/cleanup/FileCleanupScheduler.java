package ru.LevLezhnin.NauJava.job.file.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupScheduler.class);

    private final JobOperator jobOperator;
    private final Job fileCleanupJob;

    @Value("${app.files.cleanup.enabled:true}")
    private volatile boolean isCleanupJobEnabled;

    public void setCleanupJobEnabled(boolean cleanupJobEnabled) {
        isCleanupJobEnabled = cleanupJobEnabled;
    }

    @Autowired
    public FileCleanupScheduler(JobOperator jobOperator, Job fileCleanupJob) {
        this.jobOperator = jobOperator;
        this.fileCleanupJob = fileCleanupJob;
    }

    @Scheduled(cron = "${app.files.cleanup.cron:0 0 3 * * *}", scheduler = "fileCleanupSchedulerThreadPool")
    public void run() {
        executeIfEnabled();
    }

    private void executeIfEnabled() {
        if (!isCleanupJobEnabled) {
            log.info("Job очистки файлов отключена через настройку");
            return;
        }
        executeJob();
    }

    private void executeJob() {
        try {
            log.info("Запуск job-ы очистки файлов");
            var execution = jobOperator.start(fileCleanupJob, new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters());
            log.info("Job-а очистки файлов успешно запущена с execution id: {}", execution.getId());
        } catch (Exception e) {
            log.error("Не удалось запустить job-у очистки файлов", e);
        }
    }
}
