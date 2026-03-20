package ru.LevLezhnin.NauJava.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_bans")
public class UserBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "banned_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant bannedAt;

    @Column(name = "unbanned_at")
    private Instant unbannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_user_id")
    private User bannedUser;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(Instant bannedAt) {
        this.bannedAt = bannedAt;
    }

    public Instant getUnbannedAt() {
        return unbannedAt;
    }

    public void setUnbannedAt(Instant unbannedAt) {
        this.unbannedAt = unbannedAt;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        if (admin != null && admin.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Не могу изменить ответственного за бан на пользователя без прав администратора");
        }
        this.admin = admin;
    }

    public User getBannedUser() {
        return bannedUser;
    }

    public void setBannedUser(User bannedUser) {
        if (bannedUser != null && bannedUser.getRole() != UserRole.USER) {
            throw new IllegalArgumentException("Бан пользователей с правами выше USER должен происходить посредством отбирания этих прав");
        }
        this.bannedUser = bannedUser;
    }

    public UserBan() {}

    public UserBan(Long id, String reason, Instant bannedAt, Instant unbannedAt, User admin, User bannedUser) {
        this.id = id;
        this.reason = reason;
        this.bannedAt = bannedAt;
        this.unbannedAt = unbannedAt;
        this.admin = admin;
        this.bannedUser = bannedUser;
    }

    @Override
    public String toString() {
        return "UserBan{" +
                "id=" + id +
                ", reason='" + reason + '\'' +
                ", bannedAt=" + bannedAt +
                ", unbannedAt=" + unbannedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserBan userBan = (UserBan) o;
        return Objects.equals(id, userBan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private Long id;
        private String reason;
        private Instant bannedAt;
        private Instant unbannedAt;
        private User admin;
        private User bannedUser;

        public Long getId() {
            return id;
        }

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public String getReason() {
            return reason;
        }

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Instant getBannedAt() {
            return bannedAt;
        }

        public Builder setBannedAt(Instant bannedAt) {
            this.bannedAt = bannedAt;
            return this;
        }

        public Instant getUnbannedAt() {
            return unbannedAt;
        }

        public Builder setUnbannedAt(Instant unbannedAt) {
            this.unbannedAt = unbannedAt;
            return this;
        }

        public User getAdmin() {
            return admin;
        }

        public Builder setAdmin(User admin) {
            this.admin = admin;
            return this;
        }

        public User getBannedUser() {
            return bannedUser;
        }

        public Builder setBannedUser(User bannedUser) {
            this.bannedUser = bannedUser;
            return this;
        }

        public UserBan build() {
            return new UserBan(id, reason, bannedAt, unbannedAt, admin, bannedUser);
        }
    }
}
