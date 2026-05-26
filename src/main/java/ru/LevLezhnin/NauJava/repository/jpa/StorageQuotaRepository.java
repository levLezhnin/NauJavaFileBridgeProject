package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.LevLezhnin.NauJava.model.StorageQuota;

import java.util.Optional;

/**
 * Репозиторий для доступа к квотам хранения пользователей.
 * <p>
 * Предоставляет методы с pessimistic locking, необходимые для безопасного
 * изменения usedStorageBytes в конкурентной среде.
 *
 * @author Лев Лежнин
 */
public interface StorageQuotaRepository extends JpaRepository<StorageQuota, Long> {

    /**
     * Находит квоту хранения пользователя по его ID.
     * <p>
     * Реализовано через JOIN по связи User - StorageQuota (не по прямому foreign key).
     * Используется в сервисах для получения квоты текущего пользователя.
     *
     * @param userId идентификатор пользователя
     * @return Optional с квотой или empty, если пользователь не найден
     */
    @Query("SELECT u.storageQuota FROM User u WHERE u.id = :userId")
    Optional<StorageQuota> findByUserId(Long userId);

    /**
     * Загружает квоту с pessimistic write lock для атомарного обновления usedStorageBytes.
     * <p>
     * Критически важно для предотвращения race condition при одновременной загрузке/удалении файлов.
     * Используется в {@code StorageQuotaServiceImpl#updateStorageQuota}.
     *
     * @param storageQuotaId id квоты
     * @return Optional с заблокированной для обновления квотой
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StorageQuota> findForUpdateById(Long storageQuotaId);
}
