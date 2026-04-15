package ru.LevLezhnin.NauJava.service.base;

import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.model.Report;
import ru.LevLezhnin.NauJava.model.ReportStatus;
import ru.LevLezhnin.NauJava.repository.jpa.ReportRepository;
import ru.LevLezhnin.NauJava.service.interfaces.ReportService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public abstract class AbstractReportService implements ReportService {

    protected final ReportRepository reportRepository;


    protected AbstractReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
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

    protected abstract String fetchContent(Long id);

    protected Report findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Отчёт с id: %d не найден".formatted(id)));
    }

    protected abstract void updateReport(Long reportId, InputStream content, long size, ReportStatus status);

    protected void updateReportError(Long reportId, String errorMessage) {
        byte[] errorMessageBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
        long size = errorMessageBytes.length;
        updateReport(reportId, new ByteArrayInputStream(errorMessageBytes), size, ReportStatus.ERROR);
    }
}
