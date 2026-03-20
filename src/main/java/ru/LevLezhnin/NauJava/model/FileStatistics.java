package ru.LevLezhnin.NauJava.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "file_statistics")
public class FileStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "times_downloaded", nullable = false)
    private Long timesDownloaded;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "last_downloaded_at")
    private Instant lastDownloadedAt;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "fileStatistics")
    private File file;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTimesDownloaded() {
        return timesDownloaded;
    }

    public void setTimesDownloaded(Long timesDownloaded) {
        this.timesDownloaded = timesDownloaded;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getLastDownloadedAt() {
        return lastDownloadedAt;
    }

    public void setLastDownloadedAt(Instant lastDownloadedAt) {
        this.lastDownloadedAt = lastDownloadedAt;
    }

    public File getFile() {
        return file;
    }

    public FileStatistics() {
        this.timesDownloaded = 0L;
    }

    public FileStatistics(Long id, Long timesDownloaded, Long sizeBytes, Instant lastDownloadedAt, File file) {
        this();
        this.id = id;
        this.timesDownloaded = timesDownloaded;
        this.sizeBytes = sizeBytes;
        this.lastDownloadedAt = lastDownloadedAt;
        this.file = file;
    }

    @Override
    public String toString() {
        return "FileStatistics{" +
                "id=" + id +
                ", timesDownloaded=" + timesDownloaded +
                ", sizeBytes=" + sizeBytes +
                ", lastDownloadedAt=" + lastDownloadedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileStatistics that = (FileStatistics) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static FileStatistics.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long timesDownloaded;
        private Long sizeBytes;
        private Instant lastDownloadedAt;
        private File file;

        public Long getId() {
            return id;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Long getTimesDownloaded() {
            return timesDownloaded;
        }

        public Builder setTimesDownloaded(Long timesDownloaded) {
            this.timesDownloaded = timesDownloaded;
            return this;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public Builder setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }

        public Instant getLastDownloadedAt() {
            return lastDownloadedAt;
        }

        public Builder setLastDownloadedAt(Instant lastDownloadedAt) {
            this.lastDownloadedAt = lastDownloadedAt;
            return this;
        }

        public File getFile() {
            return file;
        }

        public Builder setFile(File file) {
            this.file = file;
            return this;
        }

        public FileStatistics build() {
            return new FileStatistics(id, timesDownloaded, sizeBytes, lastDownloadedAt, file);
        }
    }
}
