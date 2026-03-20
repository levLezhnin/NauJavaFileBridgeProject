package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.LevLezhnin.NauJava.model.StorageQuota;

public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {
    StorageQuota findByUserId(Long userId);
}
