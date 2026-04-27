package ru.LevLezhnin.NauJava.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "files")
public class File {

    @Id
    private UUID id;

    @Column(name = "path", columnDefinition = "TEXT", nullable = false, unique = true)
    private String path;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant uploadedAt;

    @Column(name = "expire_at", nullable = false)
    private Instant expireAt;

    @Column(name = "max_downloads", nullable = false, updatable = false)
    private Long maxDownloads;

    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "statistics_id", nullable = false, updatable = false, unique = true)
    private FileStatistics fileStatistics;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Instant expireAt) {
        this.expireAt = expireAt;
    }

    public Long getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Long maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public FileStatistics getFileStatistics() {
        return fileStatistics;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public File() {}

    public File(UUID id, String path, String name, String mimeType, Instant uploadedAt, Instant expireAt, Long maxDownloads, String passwordHash, User author, FileStatistics fileStatistics) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.mimeType = mimeType;
        this.uploadedAt = uploadedAt;
        this.expireAt = expireAt;
        this.maxDownloads = maxDownloads;
        this.passwordHash = passwordHash;
        this.author = author;
        this.fileStatistics = fileStatistics;
    }

    @Override
    public String toString() {
        return "File{" +
                "id='" + id.toString() + '\'' +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", uploadDate=" + uploadedAt +
                ", expireDate=" + expireAt +
                ", maxDownloads=" + maxDownloads +
                ", passwordHash='[ЗАЩИЩЕНО]'" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File file = (File) o;
        return Objects.equals(id, file.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static File.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String path;
        private String name;
        private String mimeType;
        private Instant uploadedAt;
        private Instant expireAt;
        private Long maxDownloads;
        private String passwordHash;
        private User author;
        private FileStatistics fileStatistics;

        public UUID getId() {
            return id;
        }

        public Builder setId(UUID id) {
            this.id = id;
            return this;
        }

        public String getPath() {
            return path;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public String getName() {
            return name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Instant getUploadedAt() {
            return uploadedAt;
        }

        public Builder setUploadedAt(Instant uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public Instant getExpireAt() {
            return expireAt;
        }

        public Builder setExpireAt(Instant expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        public Long getMaxDownloads() {
            return maxDownloads;
        }

        public Builder setMaxDownloads(Long maxDownloads) {
            this.maxDownloads = maxDownloads;
            return this;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public User getAuthor() {
            return author;
        }

        public Builder setAuthor(User author) {
            this.author = author;
            return this;
        }

        public FileStatistics getFileStatistics() {
            return fileStatistics;
        }

        public Builder setFileStatistics(FileStatistics fileStatistics) {
            this.fileStatistics = fileStatistics;
            return this;
        }

        public File build() {
            return new File(id, path, name, mimeType, uploadedAt, expireAt, maxDownloads, passwordHash, author, fileStatistics);
        }
    }
}
