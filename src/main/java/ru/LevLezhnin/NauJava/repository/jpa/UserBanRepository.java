package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ru.LevLezhnin.NauJava.model.UserBan;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource
public interface UserBanRepository extends JpaRepository<UserBan, Long> {

    @Query("FROM UserBan WHERE bannedUser.id = :userId ORDER BY bannedAt DESC")
    List<UserBan> findUserBanHistory(Long userId, Pageable pageable);

    @Query("FROM UserBan WHERE bannedUser.id = :userId AND unbannedAt IS NULL")
    Optional<UserBan> findActiveUserBan(Long userId);

}
