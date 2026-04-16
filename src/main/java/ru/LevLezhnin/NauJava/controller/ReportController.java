package ru.LevLezhnin.NauJava.controller;

import org.springframework.web.bind.annotation.*;
import ru.LevLezhnin.NauJava.service.interfaces.ReportService;

@RestController
@RequestMapping("/api/v1/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate")
    public Long generateReport() {
        Long id = reportService.createReport();
        reportService.generateReportAsync(id);
        return id;
    }

    @GetMapping("/{id}")
    public String getReportContentById(@PathVariable Long id) {
        return reportService.getReportContent(id);
    }
}
