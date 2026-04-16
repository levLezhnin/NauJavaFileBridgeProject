package ru.LevLezhnin.NauJava.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "generated_at")
    private Instant generatedAt;

    public Long getId() {
        return id;
    }

    public ReportStatus getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(ReportStatus reportStatus) {
        this.reportStatus = reportStatus;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Report() {}

    public Report(Long id, ReportStatus reportStatus, String content, Instant createdAt, Instant generatedAt) {
        this.id = id;
        this.reportStatus = reportStatus;
        this.content = content;
        this.createdAt = createdAt;
        this.generatedAt = generatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return Objects.equals(id, report.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Report.Builder builder() {
        return new Report.Builder();
    }

    public static class Builder {
        private Long id;
        private ReportStatus reportStatus;
        private String content;
        private Instant createdAt;
        private Instant generatedAt;

        public Long getId() {
            return id;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public ReportStatus getReportStatus() {
            return reportStatus;
        }

        public Builder setReportStatus(ReportStatus reportStatus) {
            this.reportStatus = reportStatus;
            return this;
        }

        public String getContent() {
            return content;
        }

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public Builder setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Instant getGeneratedAt() {
            return generatedAt;
        }

        public Builder setGeneratedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Report build() {
            return new Report(id, reportStatus, content, createdAt, generatedAt);
        }
    }
}
