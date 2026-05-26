package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import ru.LevLezhnin.NauJava.job.cleanup.FileCleanupRecord;
import ru.LevLezhnin.NauJava.model.File;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-репозиторий для сущности {@link File}.
 * <p>
 * Поддерживает pessimistic locking для конкурентной работы с лимитами скачиваний
 * и квотами, а также native query для batch-очистки просроченных файлов.
 *
 * @author Лев Лежнин
 */
public interface FileRepository extends JpaRepository<File, UUID>, JpaSpecificationExecutor<File> {

    /**
     * Загружает файл вместе со статистикой, автором и квотой автора (через EntityGraph).
     * <p>
     * Используется при формировании ссылок на скачивание и обычном получении метаданных.
     */
    @EntityGraph(attributePaths = {"fileStatistics", "author", "author.storageQuota"})
    Optional<File> findWithDetailsById(UUID fileId);

    /**
     * То же, что {@link #findWithDetailsById(UUID)}, но с pessimistic write lock.
     * <p>
     * Используется при скачивании файла для атомарной проверки и увеличения счётчика скачиваний.
     */
    @EntityGraph(attributePaths = {"fileStatistics", "author", "author.storageQuota"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<File> findForUpdateWithDetailsById(UUID fileId);

    /**
     * Загружает только сущность File с pessimistic write lock (без связанных сущностей).
     * <p>
     * Используется при удалении файла.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<File> findForUpdateById(UUID fileId);

    /**
     * Возвращает файлы конкретного пользователя, отсортированные по дате загрузки убыв.
     * <p>
     * Используется в {@code FileService#getCurrentUserFiles}.
     */
    List<File> findAllByAuthorIdOrderByUploadedAtDesc(Long authorId, Pageable pageable);

    /**
     * Native query, которая выбирает просроченные файлы с блокировкой (FOR UPDATE SKIP LOCKED).
     * <p>
     * <b>Ключевые особенности контракта:</b>
     * <ul>
     *   <li>Возвращает projection {@link FileCleanupRecord} для Batch Job</li>
     *   <li>SKIP LOCKED - позволяет нескольким инстансам приложения работать параллельно без блокировки друг друга</li>
     *   <li>Используется только планировщиком очистки</li>
     * </ul>
     *
     * @param limit максимальное количество файлов для обработки за один запуск
     */
    @Query(value = """
    SELECT f.id as fileId,
           f.path as path,
           f.expire_at as expireAt,
           f.max_downloads as maxDownloads,
           fs.times_downloaded as timesDownloaded,
           f.author_id as authorId,
           fs.size_bytes as fileSizeBytes
    FROM files f
    JOIN file_statistics fs ON f.statistics_id = fs.id
    WHERE f.expire_at < now()
    ORDER BY f.id
    LIMIT :limit
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<FileCleanupRecord> lockExpiredFiles(int limit);

    /**
     * Переопределение с явным EntityGraph + поддержкой Specification.
     * <p>
     * Необходимо, потому что стандартный findAll(Specification) не подгружает ленивые связи.
     * Используется при административном поиске файлов.
     */
    @EntityGraph(attributePaths = {"fileStatistics", "author"})
    @NotNull
    Page<File> findAll(@NotNull Specification<File> spec, @NotNull Pageable pageable);
}
