package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.LevLezhnin.NauJava.model.UserBan;

import java.util.Optional;

public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    @Query("SELECT ub FROM UserBan ub WHERE ub.bannedUser.id = :userId ORDER BY ub.bannedAt DESC")
    Page<UserBan> findUserBanHistory(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT ub FROM UserBan ub WHERE ub.admin.id = :adminId ORDER BY ub.bannedAt DESC")
    Page<UserBan> findIssuedBanHistory(@Param("adminId") Long adminId, Pageable pageable);

    @Query("FROM UserBan WHERE bannedUser.id = :userId AND unbannedAt IS NULL")
    Optional<UserBan> findActiveUserBan(Long userId);

    @Query("FROM UserBan WHERE bannedUser.id = :userId AND unbannedAt IS NULL")
    @EntityGraph(attributePaths = {"admin"})
    Optional<UserBan> findActiveUserBanWithDetails(Long userId);

    @Query("FROM UserBan WHERE bannedUser.id = :userId AND unbannedAt IS NULL")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserBan> findActiveUserBanForUpdate(Long userId);

}
