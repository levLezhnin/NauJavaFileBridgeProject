package ru.LevLezhnin.NauJava.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", columnDefinition = "TEXT", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "registered_at", nullable = false)
    @CreationTimestamp
    private Instant registeredAt;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "storage_quota_id", unique = true, nullable = false)
    private StorageQuota storageQuota;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, mappedBy = "author")
    private List<File> activeFiles;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, mappedBy = "bannedUser")
    private List<UserBan> banHistory;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, mappedBy = "admin")
    private List<UserBan> providedBans;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public StorageQuota getStorageQuota() {
        return storageQuota;
    }

    public void setStorageQuota(StorageQuota storageQuota) {
        if (this.storageQuota != null) {
            this.storageQuota.setUser(null);
        }
        if (storageQuota != null) {
            storageQuota.setUser(this);
        }
        this.storageQuota = storageQuota;
    }

    public List<File> getActiveFiles() {
        return activeFiles;
    }

    public List<UserBan> getBanHistory() {
        return banHistory;
    }

    public List<UserBan> getProvidedBans() {
        return providedBans;
    }

    public User() {
        this.activeFiles = new ArrayList<>();
        this.banHistory = new ArrayList<>();
        this.providedBans = new ArrayList<>();
    }

    public User(
            Long id,
            String username,
            String email,
            String passwordHash,
            boolean isActive,
            UserRole role,
            Instant registeredAt,
            StorageQuota storageQuota) {
        this();
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isActive = isActive;
        this.role = role;
        this.registeredAt = registeredAt;
        this.storageQuota = storageQuota;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", passwordHash='[ЗАЩИЩЕНО]'" +
                ", isActive=" + isActive +
                ", role=" + role +
                ", registeredAt=" + registeredAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static User.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private String passwordHash;
        private boolean isActive;
        private UserRole role;
        private Instant registeredAt;
        private StorageQuota storageQuota;

        public Long getId() {
            return id;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public String getUsername() {
            return username;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public String getEmail() {
            return email;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public Builder setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public boolean isActive() {
            return isActive;
        }

        public Builder setActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public UserRole getRole() {
            return role;
        }

        public Builder setRole(UserRole role) {
            this.role = role;
            return this;
        }

        public Instant getRegisteredAt() {
            return registeredAt;
        }

        public Builder setRegisteredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public StorageQuota getStorageQuota() {
            return storageQuota;
        }

        public Builder setStorageQuota(StorageQuota storageQuota) {
            this.storageQuota = storageQuota;
            return this;
        }

        public User build() {
            return new User(id, username, email, passwordHash, isActive, role, registeredAt, storageQuota);
        }
    }
}
