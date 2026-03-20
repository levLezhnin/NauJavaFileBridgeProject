package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.LevLezhnin.NauJava.model.StorageQuota;

import java.util.Optional;

public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {
    @Query("SELECT u.storageQuota FROM User u WHERE u.id = :userId")
    Optional<StorageQuota> findByUserId(Long userId);
}
