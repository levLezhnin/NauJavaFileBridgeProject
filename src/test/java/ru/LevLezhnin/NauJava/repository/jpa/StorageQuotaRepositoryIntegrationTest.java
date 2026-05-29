package ru.LevLezhnin.NauJava.repository.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.model.UserRole;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты StorageQuotaRepository")
class StorageQuotaRepositoryIntegrationTest {

    @Autowired
    private StorageQuotaRepository storageQuotaRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private StorageQuota testQuota;

    @BeforeEach
    void setUp() {

        testQuota = StorageQuota.builder()
                .setUser(testUser)
                .setUsedStorageBytes(1024L)
                .setMaxStorageBytes(5000L)
                .build();

        testUser = User.builder()
                .setUsername("quota_test_user")
                .setEmail("quota@test.com")
                .setPasswordHash("hash")
                .setRole(UserRole.USER)
                .setActive(true)
                .setRegisteredAt(Instant.now())
                .setStorageQuota(testQuota)
                .build();

        entityManager.persist(testQuota);
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("findByUserId успешно возвращает квоту существующего пользователя")
    void shouldFindQuotaByExistingUserId() {
        Optional<StorageQuota> result = storageQuotaRepository.findByUserId(testUser.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testQuota.getId());
        assertThat(result.get().getMaxStorageBytes()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("findByUserId возвращает empty для несуществующего пользователя")
    void shouldReturnEmptyForNonExistentUserId() {
        Optional<StorageQuota> result = storageQuotaRepository.findByUserId(99999L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findForUpdateById блокирует и возвращает квоту")
    void shouldLockAndReturnQuotaForUpdate() {
        Optional<StorageQuota> result = storageQuotaRepository.findForUpdateById(testQuota.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getUsedStorageBytes()).isEqualTo(1024L);
        // Pessimistic lock проверяется на уровне БД, здесь валидируем корректность маппинга и выполнения запроса
    }

    @Test
    @DisplayName("findForUpdateById возвращает empty для несуществующего id квоты")
    void shouldReturnEmptyForInvalidQuotaId() {
        Optional<StorageQuota> result = storageQuotaRepository.findForUpdateById(99999L);
        assertThat(result).isEmpty();
    }
}