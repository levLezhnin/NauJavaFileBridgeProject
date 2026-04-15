package ru.LevLezhnin.NauJava.service.interfaces;

import java.util.concurrent.CompletableFuture;

public interface ReportService {
    Long createReport();
    String getReportContent(Long id);
    void generateReport(Long reportId);
    CompletableFuture<Void> generateReportAsync(Long reportId);
}
