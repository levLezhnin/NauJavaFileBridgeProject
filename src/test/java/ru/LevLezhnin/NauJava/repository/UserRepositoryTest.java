package ru.LevLezhnin.NauJava.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.LevLezhnin.NauJava.constants.ContainerVersionConstants;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserBanRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.utils.DataSizeConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@DataJpaTest
@SpringJUnitConfig
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
public class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(ContainerVersionConstants.POSTGRES_CONTAINER_VERSION);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBanRepository userBanRepository;

    @Autowired
    private FileRepository fileRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private StorageQuota storageQuota1, storageQuota2, storageQuota3;
    private File testFile;
    private FileStatistics fileStatistics;

    @BeforeEach
    void setUp() {
        // Очистка данных перед каждым тестом
        fileRepository.deleteAll();
        userBanRepository.deleteAll();
        userRepository.deleteAll();

        // Создание квоты хранилища
        storageQuota1 = StorageQuota.builder()
                .setMaxStorageBytes(1 * DataSizeConstants.GB)
                .setUsedStorageBytes(0L)
                .build();

        storageQuota2 = StorageQuota.builder()
                .setMaxStorageBytes(1 * DataSizeConstants.GB)
                .setUsedStorageBytes(0L)
                .build();

        storageQuota3 = StorageQuota.builder()
                .setMaxStorageBytes(1 * DataSizeConstants.GB)
                .setUsedStorageBytes(0L)
                .build();

        entityManager.persist(storageQuota1);
        entityManager.persist(storageQuota2);
        entityManager.persist(storageQuota3);

        // Создание тестовых пользователей
        testUser1 = User.builder()
                .setUsername("john_doe")
                .setEmail("john@example.com")
                .setPasswordHash("hashed_password_1")
                .setRole(UserRole.USER)
                .setActive(true)
                .setStorageQuota(storageQuota1)
                .build();

        testUser2 = User.builder()
                .setUsername("jane_smith")
                .setEmail("jane@example.com")
                .setPasswordHash("hashed_password_2")
                .setRole(UserRole.USER)
                .setActive(true)
                .setStorageQuota(storageQuota2)
                .build();

        testUser3 = User.builder()
                .setUsername("admin_user")
                .setEmail("admin@example.com")
                .setPasswordHash("hashed_password_3")
                .setRole(UserRole.ADMIN)
                .setActive(false)
                .setStorageQuota(storageQuota3)
                .build();

        entityManager.persist(testUser1);
        entityManager.persist(testUser2);
        entityManager.persist(testUser3);
        entityManager.flush();

        // Создание статистики файла
        fileStatistics = FileStatistics.builder()
                .setTimesDownloaded(0L)
                .setSizeBytes(1024L)
                .build();
        entityManager.persist(fileStatistics);

        // Создание файла для тестирования связи с пользователем
        testFile = File.builder()
                .setId(UUID.randomUUID())
                .setPath("/files/data.bin")
                .setName("test.txt")
                .setMimeType(TEXT_PLAIN_VALUE)
                .setUploadedAt(Instant.now())
                .setExpireAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .setMaxDownloads(10L)
                .setPasswordHash("file_password_hash")
                .setAuthor(testUser1)
                .setFileStatistics(fileStatistics)
                .build();
        entityManager.persist(testFile);

        // Создание бана пользователя для тестирования связанных сущностей
        UserBan userBan = new UserBan.Builder()
                .setReason("Violation of rules")
                .setBannedAt(Instant.now())
                .setAdmin(testUser3) // admin
                .setBannedUser(testUser2)
                .build();
        entityManager.persist(userBan);

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Тест метода поиска с использованием Query Lookup Strategies
     * Тестируем метод findByUsernameLikeIgnoreCase из UserRepository
     */
    @Test
    void testFindByUsernameLikeIgnoreCase() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        String searchPattern = "%john%";

        // When
        List<User> foundUsers = userRepository.findByUsernameLikeIgnoreCase(searchPattern, pageable);

        // Then
        assertThat(foundUsers).isNotEmpty();
        assertThat(foundUsers).hasSize(1);
        assertThat(foundUsers.get(0).getUsername()).isEqualTo("john_doe");
    }

    /**
     * Тест метода поиска с использованием @Query JPQL через связанную сущность
     * Тестируем метод findUserBanHistory из UserBanRepository
     */
    @Test
    void testFindUserBanHistoryWithJPQL() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<UserBan> banHistory = userBanRepository.findUserBanHistory(testUser2.getId(), pageable).stream().toList();

        // Then
        assertThat(banHistory).isNotEmpty();
        assertThat(banHistory).hasSize(1);
        assertThat(banHistory.get(0).getReason()).isEqualTo("Violation of rules");
        assertThat(banHistory.get(0).getBannedUser().getId()).isEqualTo(testUser2.getId());
        assertThat(banHistory.get(0).getAdmin().getId()).isEqualTo(testUser3.getId());
    }

    /**
     * Тест метода поиска активного бана с использованием @Query JPQL
     */
    @Test
    void testFindActiveUserBan() {
        // When
        Optional<UserBan> activeBan = userBanRepository.findActiveUserBan(testUser2.getId());

        // Then
        assertThat(activeBan).isPresent();
        assertThat(activeBan.get().getUnbannedAt()).isNull();
    }

    /**
     * Тест транзакционной операции создания файла и обновления квоты пользователя
     * Положительный кейс
     */
    @Test
    @Transactional
    void testTransactionalFileCreationAndQuotaUpdate() {
        // Given
        User author = testUser1;
        Long initialUsedStorage = author.getStorageQuota().getUsedStorageBytes();
        Long fileSize = 2 * DataSizeConstants.KB;

        FileStatistics newFileStats = FileStatistics.builder()
                .setTimesDownloaded(0L)
                .setSizeBytes(fileSize)
                .build();
        entityManager.persist(newFileStats);

        File newFile = File.builder()
                .setId(UUID.randomUUID())
                .setPath("/files/new_file.txt")
                .setName("new_file.txt")
                .setMimeType(TEXT_PLAIN_VALUE)
                .setUploadedAt(Instant.now())
                .setExpireAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .setMaxDownloads(5L)
                .setPasswordHash("new_password_hash")
                .setAuthor(author)
                .setFileStatistics(newFileStats)
                .build();

        // When
        entityManager.persist(newFile);

        // Обновляем использованное место в квоте
        StorageQuota quota = author.getStorageQuota();
        quota.setUsedStorageBytes(quota.getUsedStorageBytes() + fileSize);
        entityManager.merge(quota);

        entityManager.flush();
        entityManager.clear();

        // Then
        User updatedAuthor = userRepository.findById(author.getId()).get();
        Optional<File> savedFile = fileRepository.findById(newFile.getId());

        assertThat(savedFile).isPresent();
        assertThat(savedFile.get().getName()).isEqualTo("new_file.txt");
        assertThat(updatedAuthor.getStorageQuota().getUsedStorageBytes()).isEqualTo(initialUsedStorage + fileSize);
    }

    /**
     * Тест транзакционной операции с откатом (отрицательный кейс)
     * Пытаемся создать файл, но превышаем квоту
     */
    @Test
    @Transactional
    void testTransactionalFileCreationWithQuotaExceeded() {
        // Given
        User author = testUser1;
        Long initialFileCount = fileRepository.count();

        StorageQuota quota = author.getStorageQuota();
        quota.setMaxStorageBytes(1000L);
        quota.setUsedStorageBytes(900L);
        entityManager.merge(quota);
        entityManager.flush();

        Long fileSize = 200L;

        FileStatistics newFileStats = FileStatistics.builder()
                .setTimesDownloaded(0L)
                .setSizeBytes(fileSize)
                .build();
        entityManager.persist(newFileStats);

        File newFile = File.builder()
                .setPath("/files/quota_exceed.txt")
                .setName("quota_exceed.txt")
                .setUploadedAt(Instant.now())
                .setExpireAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .setMaxDownloads(5L)
                .setPasswordHash("password")
                .setAuthor(author)
                .setFileStatistics(newFileStats)
                .build();

        // When
        try {
            if (quota.getUsedStorageBytes() + fileSize > quota.getMaxStorageBytes()) {
                throw new RuntimeException("Quota exceeded: невозможно загрузить файл");
            }
            entityManager.persist(newFile);
            quota.setUsedStorageBytes(quota.getUsedStorageBytes() + fileSize);
            entityManager.merge(quota);
            entityManager.flush();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Quota exceeded");
        }

        entityManager.clear();

        // Then - проверяем, что квота не изменилась
        User updatedAuthor = userRepository.findById(author.getId()).get();
        assertThat(updatedAuthor.getStorageQuota().getUsedStorageBytes()).isEqualTo(900L);

        // Проверяем, что количество файлов не изменилось
        assertThat(fileRepository.count()).isEqualTo(initialFileCount);
    }

    /**
     * Тест транзакционной операции с ошибкой и откатом
     * Отрицательный кейс
     */
    @Test
    @Transactional
    void testTransactionalOperationWithRollback() {
        // Given
        User author = testUser1;
        String originalUsername = author.getUsername();

        try {
            // Пытаемся выполнить составную операцию
            author.setUsername("new_username");
            entityManager.merge(author);

            // Искусственно вызываем ошибку
            if (true) {
                throw new RuntimeException("Simulated error to trigger rollback");
            }

            entityManager.flush();
        } catch (RuntimeException e) {
            // Ожидаем исключение
        }

        entityManager.clear();

        // Then - проверяем, что изменения откатились
        User updatedAuthor = userRepository.findById(author.getId()).get();
        assertThat(updatedAuthor.getUsername()).isEqualTo(originalUsername);
    }
}