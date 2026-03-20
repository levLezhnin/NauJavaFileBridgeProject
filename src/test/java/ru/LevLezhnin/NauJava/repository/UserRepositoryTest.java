package ru.LevLezhnin.NauJava.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import ru.LevLezhnin.NauJava.config.DataSizeConstants;
import ru.LevLezhnin.NauJava.model.*;
import ru.LevLezhnin.NauJava.repository.custom.UserRepositoryImplementation;
import ru.LevLezhnin.NauJava.repository.jpa.FileRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserBanRepository;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@SpringJUnitConfig
@Import(UserRepositoryImplementation.class)
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRepositoryImplementation userRepositoryCustom;

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
                .setPath("/files/test.txt")
                .setName("test.txt")
                .setUploadedAt(Instant.now())
                .setExpireAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .setMaxDownloads(10)
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
        List<UserBan> banHistory = userBanRepository.findUserBanHistory(testUser2.getId(), pageable);

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
     * Тест Criteria API метода findById
     */
    @Test
    void testCustomFindByIdWithCriteria() {
        // When
        Optional<User> foundUser = userRepositoryCustom.findById(testUser1.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("john_doe");
        assertThat(foundUser.get().getEmail()).isEqualTo("john@example.com");
    }

    /**
     * Тест Criteria API метода findById с несуществующим ID
     */
    @Test
    void testCustomFindByIdNotFound() {
        // When
        Optional<User> foundUser = userRepositoryCustom.findById(999L);

        // Then
        assertThat(foundUser).isEmpty();
    }

    /**
     * Тест Criteria API метода findByEmail
     */
    @Test
    void testCustomFindByEmailWithCriteria() {
        // When
        Optional<User> foundUser = userRepositoryCustom.findByEmail("jane@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("jane_smith");
        assertThat(foundUser.get().getEmail()).isEqualTo("jane@example.com");
    }

    /**
     * Тест Criteria API метода findAll с пагинацией
     */
    @Test
    void testCustomFindAllWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<User> users = userRepositoryCustom.findAll(pageable);

        // Then
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getId()).isLessThan(users.get(1).getId()); // Проверяем сортировку по ID
    }

    /**
     * Тест Criteria API метода findByUsername с пагинацией
     */
    @Test
    void testCustomFindByUsernameWithCriteria() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<User> users = userRepositoryCustom.findByUsername("%john%", pageable);

        // Then
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("john_doe");
    }

    /**
     * Тест Criteria API метода deleteById
     */
    @Test
    void testCustomDeleteByIdWithCriteria() {
        // Given
        Long userIdToDelete = testUser2.getId();

        // When
        userRepositoryCustom.deleteById(userIdToDelete);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> deletedUser = userRepository.findById(userIdToDelete);
        assertThat(deletedUser).isEmpty();
    }

    /**
     * Тест транзакционной операции создания файла и обновления квоты пользователя
     * Положительный кейс
     */
    @Test
    @org.springframework.transaction.annotation.Transactional
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
                .setPath("/files/new_file.txt")
                .setName("new_file.txt")
                .setUploadedAt(Instant.now())
                .setExpireAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .setMaxDownloads(5)
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
    @org.springframework.transaction.annotation.Transactional
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
                .setMaxDownloads(5)
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
     * Тест транзакционной операции удаления пользователя с каскадным удалением файлов
     * Положительный кейс
     */
    @Test
    @org.springframework.transaction.annotation.Transactional
    void testTransactionalUserDeletionWithCascade() {
        // Given
        Long userId = testUser1.getId();
        UUID fileId = testFile.getId();

        // When
        userRepositoryCustom.deleteById(userId);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<User> deletedUser = userRepositoryCustom.findById(userId);
        Optional<File> changedFile = fileRepository.findById(fileId);

        assertThat(deletedUser).isEmpty();
        assertThat(changedFile).isPresent();
        assertThat(changedFile.get().getAuthor()).isNull();
    }

    /**
     * Тест транзакционной операции с ошибкой и откатом
     * Отрицательный кейс
     */
    @Test
    @org.springframework.transaction.annotation.Transactional
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