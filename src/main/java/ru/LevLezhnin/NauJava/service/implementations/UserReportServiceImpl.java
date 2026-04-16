package ru.LevLezhnin.NauJava.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.model.Report;
import ru.LevLezhnin.NauJava.model.ReportStatus;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.custom.ObjectStorageRepository;
import ru.LevLezhnin.NauJava.repository.jpa.ReportRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.service.interfaces.ReportService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UserReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(UserReportServiceImpl.class);
    private static final ExecutorService userReportServiceExecutor = Executors.newCachedThreadPool();
    private static final String REPORT_PATH_TEMPLATE = "/reports/%d";

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ObjectStorageRepository reportStorageRepository;
    private final TemplateEngine templateEngine;

    public UserReportServiceImpl(ReportRepository reportRepository, ObjectStorageRepository reportStorageRepository, UserRepository userRepository, TemplateEngine templateEngine) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.reportStorageRepository = reportStorageRepository;
        this.templateEngine = templateEngine;
    }

    @Override
    public Long createReport() {
        Report report = Report.builder()
                .setReportStatus(ReportStatus.CREATED)
                .setCreatedAt(Instant.now())
                .build();
        reportRepository.save(report);
        return report.getId();
    }

    @Override
    public String getReportContent(Long id) {
        Report report = findById(id);

        return switch (report.getReportStatus()) {
            case FINISHED -> fetchContent(id);
            case CREATED -> throw new IllegalStateException("Отчёт ещё создаётся");
            case ERROR -> throw new IllegalStateException("Формирование отчёта завершилось с ошибкой: %s".formatted(report.getContent()));
        };
    }

    private Report findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Отчёт с id: %d не найден".formatted(id)));
    }

    private ReportData getReportData() throws InterruptedException {

        AtomicLong usersCount = new AtomicLong();
        AtomicReference<List<User>> usersList = new AtomicReference<>();
        AtomicLong timeElapsedCountingUsers = new AtomicLong();
        AtomicLong timeElapsedCollectingUsers = new AtomicLong();
        AtomicReference<Throwable> threadException = new AtomicReference<>(null);

        Thread usersCountThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                usersCount.set(userRepository.count());
            } catch (Throwable e) {
                log.error("Поток {} завершился с ошибкой: {}", Thread.currentThread().getName(), e.getMessage());
                threadException.compareAndSet(null, e);
            } finally {
                long end = System.currentTimeMillis();
                timeElapsedCountingUsers.set(end - start);
            }
        }, "UserReportService-CountThread");
        Thread usersListThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                usersList.set(userRepository.findAll());
            } catch (Throwable e) {
                log.error("Поток {} завершился с ошибкой: {}", Thread.currentThread().getName(), e.getMessage());
                threadException.compareAndSet(null, e);
            } finally {
                long end = System.currentTimeMillis();
                timeElapsedCollectingUsers.set(end - start);
            }
        }, "UserReportService-UsersListThread");

        usersCountThread.start();
        usersListThread.start();
        usersCountThread.join();
        usersListThread.join();

        Throwable exception = threadException.get();
        if (exception != null) {
            throw new RuntimeException("Формирование отчёта завершилось с ошибкой: %s".formatted(exception.getMessage()));
        }

        return new ReportData(usersCount.get(), usersList.get(), timeElapsedCountingUsers.get(), timeElapsedCollectingUsers.get());
    }

    private String fetchContent(Long id) {
        Report report = findById(id);

        try (InputStream reportStream = reportStorageRepository.downloadByPath(report.getContent())) {
            return new String(reportStream.readAllBytes());
        } catch (IOException e) {
            log.error("Не удалось прочитать отчёт {} из хранилища", id, e);
            throw new RuntimeException("Ошибка чтения отчёта: " + id, e);
        }
    }

    @Override
    public void generateReport(Long reportId) {
        try {
            long start = System.currentTimeMillis();

            ReportData reportData = getReportData();

            Context context = new Context();
            context.setVariable("usersCount", reportData.usersCount());
            context.setVariable("timeElapsedCountingUsersMillis", reportData.timeElapsedCountingUsersMillis());
            context.setVariable("users", reportData.userList());
            context.setVariable("timeElapsedCollectingUsersMillis", reportData.timeElapsedCollectingUsersMillis());

            long end = System.currentTimeMillis();
            context.setVariable("totalTime", end - start);

            String html = templateEngine.process("report-content", context);
            byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
            InputStream htmlStream = new ByteArrayInputStream(htmlBytes);
            updateReport(reportId, htmlStream, htmlBytes.length, ReportStatus.FINISHED);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateReportError(reportId, "Формирование отчёта было прервано");
        } catch (Exception e) {
            log.error(e.getMessage());
            updateReportError(reportId, "Формирование отчёта завершилось с ошибкой");
        }
    }

    @Override
    public CompletableFuture<Void> generateReportAsync(Long reportId) {
        return CompletableFuture.runAsync(
                () -> generateReport(reportId),
                userReportServiceExecutor);
    }

    @Transactional
    private void updateReport(Long reportId, InputStream content, long size, ReportStatus status) {
        Report report = findById(reportId);

        String path = REPORT_PATH_TEMPLATE.formatted(reportId);

        try {
            reportStorageRepository.uploadWithPath(path, content, size, MediaType.TEXT_HTML_VALUE);
        } catch (Throwable e) {
            log.error("Ошибка при загрузке отчёта в объектное хранилище", e);
            updateReportError(reportId, "Не получилось загризить отчёт в объектное хранилище");
        }

        report.setContent(path);
        report.setReportStatus(status);
        report.setGeneratedAt(Instant.now());

        reportRepository.save(report);
    }

    private void updateReportError(Long reportId, String errorMessage) {
        byte[] errorMessageBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
        long size = errorMessageBytes.length;
        updateReport(reportId, new ByteArrayInputStream(errorMessageBytes), size, ReportStatus.ERROR);
    }

    private record ReportData(long usersCount, List<User> userList, long timeElapsedCountingUsersMillis, long timeElapsedCollectingUsersMillis){}
}
