package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.LevLezhnin.NauJava.model.FileStatistics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FileStatisticsRepository extends JpaRepository<FileStatistics, Long> {
    @Modifying
    @Query("""
            UPDATE FileStatistics fs
            SET fs.timesDownloaded = fs.timesDownloaded + 1,
                fs.lastDownloadedAt = :now
            WHERE fs.id = :statsId
                AND fs.timesDownloaded < :maxDownloads
                AND fs.file.expireAt > :now
            """)
    int onDownload(@Param("statsId") Long statsId,
                   @Param("maxDownloads") Long maxDownloads,
                   @Param("now") Instant now);

    @Query("SELECT f.fileStatistics.id FROM File f WHERE f.id IN :fileIds")
    List<Long> findStatisticsIdsByFileIds(@Param("fileIds") Iterable<UUID> fileIds);
}
