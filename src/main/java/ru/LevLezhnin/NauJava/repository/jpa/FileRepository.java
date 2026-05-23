package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.LevLezhnin.NauJava.job.file.cleanup.FileCleanupRecord;
import ru.LevLezhnin.NauJava.model.File;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    @EntityGraph(attributePaths = {"fileStatistics", "author", "author.storageQuota"})
    Optional<File> findWithDetailsById(UUID fileId);

    @EntityGraph(attributePaths = {"fileStatistics", "author", "author.storageQuota"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<File> findForUpdateWithDetailsById(UUID fileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<File> findForUpdateById(UUID fileId);

    List<File> findAllByAuthorIdOrderByUploadedAtDesc(Long authorId, Pageable pageable);

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
}
