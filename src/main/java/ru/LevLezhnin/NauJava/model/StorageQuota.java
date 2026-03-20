package ru.LevLezhnin.NauJava.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "storage_quotas")
public class StorageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "max_storage_bytes", nullable = false)
    private Long maxStorageBytes;

    @Column(name = "used_storage_bytes", nullable = false)
    private Long usedStorageBytes;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @OneToOne(mappedBy = "storageQuota", fetch = FetchType.LAZY)
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMaxStorageBytes() {
        return maxStorageBytes;
    }

    public void setMaxStorageBytes(Long maxStorageBytes) {
        this.maxStorageBytes = maxStorageBytes;
    }

    public Long getUsedStorageBytes() {
        return usedStorageBytes;
    }

    public void setUsedStorageBytes(Long usedStorageBytes) {
        this.usedStorageBytes = usedStorageBytes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public StorageQuota() {
        this.usedStorageBytes = 0L;
    }

    public StorageQuota(Long id, Long maxStorageBytes, Long usedStorageBytes, Instant updatedAt, User user) {
        this.id = id;
        this.maxStorageBytes = maxStorageBytes;
        this.usedStorageBytes = usedStorageBytes;
        this.updatedAt = updatedAt;
        this.user = user;
    }

    @Override
    public String toString() {
        return "StorageQuota{" +
                "id=" + id +
                ", maxStorageBytes=" + maxStorageBytes +
                ", usedStorageBytes=" + usedStorageBytes +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageQuota that = (StorageQuota) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static StorageQuota.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long maxStorageBytes;
        private Long usedStorageBytes;
        private Instant updatedAt;
        private User user;

        public Long getId() {
            return id;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Long getMaxStorageBytes() {
            return maxStorageBytes;
        }

        public Builder setMaxStorageBytes(Long maxStorageBytes) {
            this.maxStorageBytes = maxStorageBytes;
            return this;
        }

        public Long getUsedStorageBytes() {
            return usedStorageBytes;
        }

        public Builder setUsedStorageBytes(Long usedStorageBytes) {
            this.usedStorageBytes = usedStorageBytes;
            return this;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public Builder setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public User getUser() {
            return user;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public StorageQuota build() {
            return new StorageQuota(id, maxStorageBytes, usedStorageBytes, updatedAt, user);
        }
    }
}
