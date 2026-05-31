package ru.LevLezhnin.NauJava.service.implementations;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.LevLezhnin.NauJava.config.properties.QuotaProperties;
import ru.LevLezhnin.NauJava.dto.storageQuotas.StorageQuotaResponseDto;
import ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exception.storagequotas.StorageQuotaExceededException;
import ru.LevLezhnin.NauJava.metrics.StorageQuotaMetrics;
import ru.LevLezhnin.NauJava.model.StorageQuota;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.jpa.StorageQuotaRepository;
import ru.LevLezhnin.NauJava.security.context.RequestContextService;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceImplUnitTest {

    @Mock
    private StorageQuotaRepository storageQuotaRepository;
    @Mock
    private RequestContextService requestContextService;
    @Mock
    private QuotaProperties quotaProperties;

    private StorageQuotaServiceImpl storageQuotaService;

    private final Long testQuotaId = 1L;
    private final Long testUserId = 42L;

    private User testUser;

    private final long maxStorageBytes = 1000L;
    private final long initialUsedBytes = 100L;

    private StorageQuota testStorageQuota;

    @BeforeEach
    void setUp() {

        storageQuotaService = new StorageQuotaServiceImpl(
                quotaProperties,
                storageQuotaRepository,
                requestContextService,
                new StorageQuotaMetrics(new SimpleMeterRegistry())
        );

        testUser = User.builder()
                .setId(testUserId)
                .build();

        testStorageQuota = StorageQuota.builder()
                .setId(testQuotaId)
                .setUser(testUser)
                .setUsedStorageBytes(initialUsedBytes)
                .setMaxStorageBytes(maxStorageBytes)
                .setUpdatedAt(Instant.now().minusSeconds(3600))
                .build();
    }

    @Nested
    @DisplayName("updateStorageQuota")
    class UpdateStorageQuotaTests {

        @Test
        @DisplayName("Позитивный тест: успешное обновление квоты в пределах лимита")
        void shouldUpdateQuota_whenDeltaWithinLimits() {
            long deltaBytes = 50L;
            long expectedUsedBytes = initialUsedBytes + deltaBytes;

            when(storageQuotaRepository.findForUpdateById(eq(testQuotaId)))
                    .thenReturn(Optional.of(testStorageQuota));
            when(storageQuotaRepository.save(any(StorageQuota.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            storageQuotaService.updateStorageQuota(testQuotaId, deltaBytes);

            assertEquals(expectedUsedBytes, testStorageQuota.getUsedStorageBytes());
            assertNotNull(testStorageQuota.getUpdatedAt());
            verify(storageQuotaRepository, times(1)).findForUpdateById(eq(testQuotaId));
            verify(storageQuotaRepository, times(1)).save(eq(testStorageQuota));
        }

        @Test
        @DisplayName("Позитивный тест: уменьшение квоты (отрицательная дельта)")
        void shouldUpdateQuota_whenNegativeDelta() {
            long deltaBytes = -30L;
            long expectedUsedBytes = initialUsedBytes + deltaBytes;

            when(storageQuotaRepository.findForUpdateById(eq(testQuotaId)))
                    .thenReturn(Optional.of(testStorageQuota));
            when(storageQuotaRepository.save(any(StorageQuota.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            storageQuotaService.updateStorageQuota(testQuotaId, deltaBytes);

            assertEquals(expectedUsedBytes, testStorageQuota.getUsedStorageBytes());
            verify(storageQuotaRepository, times(1)).save(eq(testStorageQuota));
        }

        @Test
        @DisplayName("Позитивный тест: попытка установить отрицательное значение -> квота обнуляется")
        void shouldClampToZero_whenNegativeResult() {
            long deltaBytes = -200L; // 100 - 200 = -100

            when(storageQuotaRepository.findForUpdateById(eq(testQuotaId)))
                    .thenReturn(Optional.of(testStorageQuota));
            when(storageQuotaRepository.save(any(StorageQuota.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            storageQuotaService.updateStorageQuota(testQuotaId, deltaBytes);

            assertEquals(0L, testStorageQuota.getUsedStorageBytes(),
                    "Заполненность не должна быть отрицательной");
            verify(storageQuotaRepository, times(1)).save(eq(testStorageQuota));
        }

        @Test
        @DisplayName("Негативный тест: превышение лимита хранилища")
        void shouldThrowExceededException_whenNewUsedBytesExceedMax() {
            long deltaBytes = 950L; // 100 + 950 = 1050 > 1000

            when(storageQuotaRepository.findForUpdateById(eq(testQuotaId)))
                    .thenReturn(Optional.of(testStorageQuota));

            StorageQuotaExceededException exception = assertThrows(
                    StorageQuotaExceededException.class,
                    () -> storageQuotaService.updateStorageQuota(testQuotaId, deltaBytes)
            );
            assertTrue(exception.getMessage().contains("Превышен лимит хранилища"));
            verify(storageQuotaRepository, times(1)).findForUpdateById(eq(testQuotaId));
            verify(storageQuotaRepository, never()).save(any(StorageQuota.class));

            assertEquals(initialUsedBytes, testStorageQuota.getUsedStorageBytes());
        }

        @Test
        @DisplayName("Негативный тест: квота не найдена по ID")
        void shouldThrowEntityNotFoundException_whenQuotaNotFound() {
            when(storageQuotaRepository.findForUpdateById(eq(testQuotaId)))
                    .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(
                    EntityNotFoundException.class,
                    () -> storageQuotaService.updateStorageQuota(testQuotaId, 50L)
            );
            assertTrue(exception.getMessage().contains("Квота с id: " + testQuotaId + " не найдена"));
            verify(storageQuotaRepository, times(1)).findForUpdateById(eq(testQuotaId));
            verify(storageQuotaRepository, never()).save(any(StorageQuota.class));
        }

        @Test
        @DisplayName("Позитивный тест: обновление до точного лимита")
        void shouldUpdateQuota_whenNewUsedBytesEqualsMax() {
            long deltaBytes = maxStorageBytes - initialUsedBytes; // ровно до лимита

            when(storageQuotaRepository.findForUpdateById(eq(testQuotaId)))
                    .thenReturn(Optional.of(testStorageQuota));
            when(storageQuotaRepository.save(any(StorageQuota.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            storageQuotaService.updateStorageQuota(testQuotaId, deltaBytes);

            assertEquals(maxStorageBytes, testStorageQuota.getUsedStorageBytes());
            verify(storageQuotaRepository, times(1)).save(eq(testStorageQuota));
        }

        @Test
        @DisplayName("Проверка: обновляется поле updatedAt при успешном сохранении")
        void shouldUpdateTimestamp_whenQuotaUpdated() {
            Instant beforeUpdate = Instant.now();
            long deltaBytes = 10L;

            when(storageQuotaRepository.findForUpdateById(eq(testQuotaId)))
                    .thenReturn(Optional.of(testStorageQuota));
            when(storageQuotaRepository.save(any(StorageQuota.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            storageQuotaService.updateStorageQuota(testQuotaId, deltaBytes);

            Instant updatedAt = testStorageQuota.getUpdatedAt();
            assertNotNull(updatedAt);
            assertTrue(updatedAt.isAfter(beforeUpdate.minusSeconds(1)),
                    "updatedAt должен быть установлен в момент обновления");
            assertTrue(updatedAt.isBefore(Instant.now().plusSeconds(1)),
                    "updatedAt не должен быть из будущего");
        }
    }

    @Nested
    @DisplayName("getCurrentUserStorageQuota")
    class GetCurrentUserStorageQuotaTests {

        @Test
        @DisplayName("Позитивный тест: возврат корректного DTO для текущего пользователя")
        void shouldReturnStorageQuotaDto_whenQuotaExists() {
            when(requestContextService.getUserId()).thenReturn(testUserId);
            when(storageQuotaRepository.findByUserId(eq(testUserId)))
                    .thenReturn(Optional.of(testStorageQuota));

            StorageQuotaResponseDto result = storageQuotaService.getCurrentUserStorageQuota();

            assertAll(
                    () -> assertNotNull(result, "DTO не должен быть null"),
                    () -> assertEquals(String.valueOf(initialUsedBytes), result.usedStorageBytes(),
                            "usedStorageBytes не совпадает"),
                    () -> assertEquals(String.valueOf(maxStorageBytes), result.maxStorageBytes(),
                            "maxStorageBytes не совпадает")
            );
            verify(requestContextService, times(1)).getUserId();
            verify(storageQuotaRepository, times(1)).findByUserId(eq(testUserId));
        }

        @Test
        @DisplayName("Позитивный тест: значения возвращаются как строки (для избежания проблем с большими числами)")
        void shouldReturnValuesAsString_whenLargeNumbers() {
            long largeUsed = 9_223_372_036_854_775_800L; // близко к Long.MAX_VALUE
            long largeMax = 9_223_372_036_854_775_807L; // Long.MAX_VALUE

            StorageQuota largeQuota = StorageQuota.builder()
                    .setId(testQuotaId)
                    .setUser(testUser)
                    .setUsedStorageBytes(largeUsed)
                    .setMaxStorageBytes(largeMax)
                    .build();

            when(requestContextService.getUserId()).thenReturn(testUserId);
            when(storageQuotaRepository.findByUserId(eq(testUserId)))
                    .thenReturn(Optional.of(largeQuota));

            StorageQuotaResponseDto result = storageQuotaService.getCurrentUserStorageQuota();

            assertAll(
                    () -> assertEquals(String.valueOf(largeUsed), result.usedStorageBytes()),
                    () -> assertEquals(String.valueOf(largeMax), result.maxStorageBytes())
            );
        }

        @Test
        @DisplayName("Негативный тест: квота не найдена для текущего пользователя")
        void shouldThrowEntityNotFoundException_whenUserQuotaNotFound() {
            when(requestContextService.getUserId()).thenReturn(testUserId);
            when(storageQuotaRepository.findByUserId(eq(testUserId)))
                    .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(
                    EntityNotFoundException.class,
                    () -> storageQuotaService.getCurrentUserStorageQuota()
            );
            assertTrue(exception.getMessage().contains("Квота для пользователя с id: " + testUserId + " не найдена"));
            verify(requestContextService, times(1)).getUserId();
            verify(storageQuotaRepository, times(1)).findByUserId(eq(testUserId));
        }

        @Test
        @DisplayName("Проверка: getUserId вызывается ровно один раз")
        void shouldCallGetUserIdOnce_whenFetchingCurrentUserQuota() {
            when(requestContextService.getUserId()).thenReturn(testUserId);
            when(storageQuotaRepository.findByUserId(eq(testUserId)))
                    .thenReturn(Optional.of(testStorageQuota));

            storageQuotaService.getCurrentUserStorageQuota();

            verify(requestContextService, times(1)).getUserId();
        }
    }
}