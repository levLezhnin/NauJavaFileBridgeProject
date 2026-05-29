package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.model.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты FileStatisticsRepository")
class FileStatisticsRepositoryIntegrationTest {

    @Autowired
    private FileStatisticsRepository fileStatisticsRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private StorageQuota quota;
    private File testFile;
    private FileStatistics testStats;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        quota = StorageQuota.builder()
                .setUser(user)
                .setMaxStorageBytes(10000L)
                .setUsedStorageBytes(0L)
                .build();

        user = User.builder()
                .setUsername("stats_user")
                .setEmail("stats@test.com")
                .setPasswordHash("hash")
                .setRole(UserRole.USER)
                .setActive(true)
                .setRegisteredAt(now)
                .setStorageQuota(quota)
                .build();

        entityManager.persist(quota);
        entityManager.persist(user);

        testStats = FileStatistics.builder()
                .setSizeBytes(500L)
                .setTimesDownloaded(0L)
                .setLastDownloadedAt(null)
                .build();
        entityManager.persist(testStats);

        testFile = File.builder()
                .setId(UUID.randomUUID())
                .setPath("/files/test.bin")
                .setName("test.bin")
                .setMimeType("application/octet-stream")
                .setUploadedAt(now)
                .setExpireAt(now.plus(Duration.ofDays(1)))
                .setMaxDownloads(5L)
                .setPasswordHash("hash")
                .setAuthor(user)
                .setFileStatistics(testStats)
                .build();
        entityManager.persist(testFile);
        entityManager.flush();
    }

    @Test
    @DisplayName("onDownload успешно инкрементирует счётчик и обновляет время")
    void shouldIncrementCounterWhenUnderLimitAndActive() {

        testStats.setLastDownloadedAt(now.minusSeconds(100));
        entityManager.persist(testStats);
        entityManager.flush();

        int updated = fileStatisticsRepository.onDownload(testStats.getId(), testFile.getMaxDownloads(), now);

        assertThat(updated).isEqualTo(1);

        entityManager.refresh(testStats);
        assertThat(testStats.getTimesDownloaded()).isEqualTo(1L);

        assertThat(testStats.getLastDownloadedAt()).isCloseTo(now, within(90, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("onDownload возвращает 0, если достигнут лимит скачиваний")
    void shouldReturnZeroWhenDownloadLimitReached() {
        testStats.setTimesDownloaded(5L);
        entityManager.merge(testStats);
        entityManager.flush();

        int updated = fileStatisticsRepository.onDownload(testStats.getId(), testFile.getMaxDownloads(), now);

        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("onDownload возвращает 0, если файл просрочен")
    void shouldReturnZeroWhenFileExpired() {
        Instant past = now.minus(Duration.ofSeconds(1));
        testFile.setExpireAt(past);
        entityManager.persist(testFile);
        entityManager.flush();

        int updated = fileStatisticsRepository.onDownload(testStats.getId(), testFile.getMaxDownloads(), now);

        assertThat(updated).isEqualTo(0);
    }

    @Test
    @DisplayName("findStatisticsIdsByFileIds возвращает корректные ID статистики")
    void shouldReturnStatisticsIdsByFileIds() {
        FileStatistics testStatsSecond = FileStatistics.builder()
                .setSizeBytes(500L)
                .setTimesDownloaded(0L)
                .setLastDownloadedAt(null)
                .build();
        entityManager.persist(testStatsSecond);

        File secondFile = File.builder()
                .setId(UUID.randomUUID())
                .setPath("/files/second.bin")
                .setName("second.bin")
                .setMimeType("text/plain")
                .setUploadedAt(now)
                .setExpireAt(now.plus(1, TimeUnit.DAYS.toChronoUnit()))
                .setMaxDownloads(1L)
                .setAuthor(testFile.getAuthor())
                .setFileStatistics(testStatsSecond)
                .build();
        entityManager.persist(secondFile);
        entityManager.flush();

        List<Long> ids = fileStatisticsRepository.findStatisticsIdsByFileIds(List.of(testFile.getId()));

        assertThat(ids).hasSize(1);
        assertThat(ids.getFirst()).isEqualTo(testStats.getId());
    }
}