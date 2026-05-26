package ru.LevLezhnin.NauJava.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.LevLezhnin.NauJava.model.FileStatistics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы со статистикой скачиваний файлов.
 * <p>
 * Содержит специальные модифицирующие запросы для атомарного увеличения счётчика скачиваний
 * и вспомогательные методы для Batch Job очистки.
 *
 * @author Лев Лежнин
 */
public interface FileStatisticsRepository extends JpaRepository<FileStatistics, Long> {

    /**
     * Атомарно увеличивает счётчик скачиваний и обновляет время последнего скачивания.
     * <p>
     * <b>Важный контракт:</b>
     * <ul>
     *   <li>Используется внутри транзакции с pessimistic lock на File</li>
     *   <li>Условия в WHERE гарантируют, что счётчик не будет увеличен, если лимит уже достигнут или файл просрочен</li>
     *   <li>Возвращает количество обновлённых строк (0 или 1). Если 0 - значит условие не прошло (конкурентный доступ или просрочка)</li>
     * </ul>
     * Это основной механизм защиты от превышения maxDownloads при одновременных скачиваниях.
     *
     * @param statsId       id записи статистики
     * @param maxDownloads  максимальное разрешённое количество скачиваний
     * @param now           текущее время (для сравнения с expireAt)
     * @return 1 если обновление прошло успешно, 0 - если условия не выполнены
     */
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

    /**
     * Возвращает идентификаторы записей статистики для списка файлов.
     * <p>
     * Используется в Batch Job очистки для массового удаления статистики вместе с файлами.
     *
     * @param fileIds коллекция UUID файлов
     * @return список id записей FileStatistics
     */
    @Query("SELECT f.fileStatistics.id FROM File f WHERE f.id IN :fileIds")
    List<Long> findStatisticsIdsByFileIds(@Param("fileIds") Iterable<UUID> fileIds);
}
