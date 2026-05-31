package ru.LevLezhnin.NauJava.service.implementations;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.LevLezhnin.NauJava.metrics.TokenBlacklistMetrics;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceInMemoryImplUnitTest {

    private TokenBlacklistServiceInMemoryImpl tokenBlacklistService;

    private final String validToken = "eyJhbGciOiJIUzI1NiIsYWNjZXNz...";
    private final String anotherToken = "different_refresh_token_here";
    private final Duration validTtl = Duration.ofMinutes(15);

    @BeforeEach
    void setUp() {
        TokenBlacklistMetrics tokenBlacklistMetrics = new TokenBlacklistMetrics(new SimpleMeterRegistry());
        tokenBlacklistService = new TokenBlacklistServiceInMemoryImpl(tokenBlacklistMetrics);
    }

    @Nested
    @DisplayName("blacklistToken")
    class BlacklistTokenTests {

        @Test
        @DisplayName("Позитивный тест: токен успешно добавляется в чёрный список")
        void shouldAddTokenToBlacklist_whenValidTokenAndTtl() {
            tokenBlacklistService.blacklistToken(validToken, validTtl);
            assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken),
                    "Токен должен определяться как отозванный");
        }

        @Test
        @DisplayName("Позитивный тест: разные токены имеют разные записи в чёрном списке")
        void shouldHandleMultipleTokensIndependently() {
            tokenBlacklistService.blacklistToken(validToken, validTtl);
            tokenBlacklistService.blacklistToken(anotherToken, Duration.ofHours(1));

            assertAll(
                    () -> assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken)),
                    () -> assertTrue(tokenBlacklistService.isTokenBlacklisted(anotherToken))
            );
        }

        @Test
        @DisplayName("Негативный тест: null-токен не добавляется в чёрный список")
        void shouldNotAddNullToken() {
            assertDoesNotThrow(() -> tokenBlacklistService.blacklistToken(null, validTtl));
            assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
        }

        @Test
        @DisplayName("Негативный тест: токен с отрицательным TTL не добавляется")
        void shouldNotAddTokenWithNegativeTtl() {
            Duration negativeTtl = Duration.ofSeconds(-10);
            tokenBlacklistService.blacklistToken(validToken, negativeTtl);
            assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken),
                    "Токен с отрицательным TTL не должен быть в чёрном списке");
        }

        @Test
        @DisplayName("Негативный тест: токен с null TTL не добавляется")
        void shouldNotAddTokenWithNullTtl() {
            tokenBlacklistService.blacklistToken(validToken, null);
            assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken));
        }

        @Test
        @DisplayName("Позитивный тест: одинаковые токены дают одинаковый хеш-ключ")
        void shouldGenerateSameKeyForSameToken() {
            tokenBlacklistService.blacklistToken(validToken, Duration.ofMinutes(1));
            tokenBlacklistService.blacklistToken(validToken, Duration.ofMinutes(5));
            assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));
        }
    }

    @Nested
    @DisplayName("isTokenBlacklisted")
    class IsTokenBlacklistedTests {

        @Test
        @DisplayName("Позитивный тест: возвращает false для токена, которого нет в списке")
        void shouldReturnFalse_whenTokenNotInBlacklist() {
            assertFalse(tokenBlacklistService.isTokenBlacklisted("unknown_token"));
        }

        @Test
        @DisplayName("Позитивный тест: возвращает false для null-токена")
        void shouldReturnFalse_whenTokenIsNull() {
            assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
        }

        @Test
        @DisplayName("Позитивный тест: возвращает true для активного токена в чёрном списке")
        void shouldReturnTrue_whenTokenIsBlacklistedAndNotExpired() {
            tokenBlacklistService.blacklistToken(validToken, Duration.ofHours(1));
            assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));
        }

        @Test
        @DisplayName("Позитивный тест: возвращает false для истёкшего токена и удаляет его")
        void shouldReturnFalseAndRemove_whenTokenExpired() throws InterruptedException {
            Duration shortTtl = Duration.ofMillis(100);
            tokenBlacklistService.blacklistToken(validToken, shortTtl);

            assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));

            Thread.sleep(150);

            assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken));
            assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken),
                    "Повторная проверка также должна вернуть false");
        }

        @Test
        @DisplayName("Позитивный тест: токены с одинаковым содержимым, но разным регистром - разные токены")
        void shouldTreatCaseSensitiveTokensAsDifferent() {
            String lower = "token";
            String upper = "TOKEN";

            tokenBlacklistService.blacklistToken(lower, validTtl);

            assertTrue(tokenBlacklistService.isTokenBlacklisted(lower));
            assertFalse(tokenBlacklistService.isTokenBlacklisted(upper),
                    "Хеширование должно учитывать регистр");
        }
    }

    @Nested
    @DisplayName("cleanUp (фоновая очистка)")
    class CleanUpTests {

        @Test
        @DisplayName("Позитивный тест: cleanUp удаляет только просроченные токены")
        void shouldRemoveOnlyExpiredTokens() throws Exception {
            // Добавляем токены с разным TTL
            Duration longTtl = Duration.ofHours(1);
            Duration shortTtl = Duration.ofMillis(50);

            String persistentToken = "persistent_token";
            String ephemeralToken = "ephemeral_token";

            tokenBlacklistService.blacklistToken(persistentToken, longTtl);
            tokenBlacklistService.blacklistToken(ephemeralToken, shortTtl);

            Thread.sleep(100);

            Method cleanUpMethod = TokenBlacklistServiceInMemoryImpl.class
                    .getDeclaredMethod("cleanUp");
            cleanUpMethod.setAccessible(true);
            cleanUpMethod.invoke(tokenBlacklistService);

            assertAll(
                    () -> assertFalse(tokenBlacklistService.isTokenBlacklisted(ephemeralToken),
                            "Просроченный токен должен быть удалён"),
                    () -> assertTrue(tokenBlacklistService.isTokenBlacklisted(persistentToken),
                            "Валидный токен должен остаться в списке")
            );
        }

        @Test
        @DisplayName("Позитивный тест: cleanUp не падает при пустом чёрном списке")
        void shouldHandleEmptyBlacklist() throws Exception {
            Method cleanUpMethod = TokenBlacklistServiceInMemoryImpl.class
                    .getDeclaredMethod("cleanUp");
            cleanUpMethod.setAccessible(true);

            assertDoesNotThrow(() -> cleanUpMethod.invoke(tokenBlacklistService));
        }

        @Test
        @DisplayName("Позитивный тест: множественные вызовы cleanUp идемпотентны")
        void shouldHandleMultipleCleanUpCalls() throws Exception {
            Method cleanUpMethod = TokenBlacklistServiceInMemoryImpl.class
                    .getDeclaredMethod("cleanUp");
            cleanUpMethod.setAccessible(true);

            tokenBlacklistService.blacklistToken(validToken, Duration.ofMillis(50));
            Thread.sleep(100);

            assertDoesNotThrow(() -> {
                cleanUpMethod.invoke(tokenBlacklistService);
                cleanUpMethod.invoke(tokenBlacklistService);
                cleanUpMethod.invoke(tokenBlacklistService);
            });

            assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken));
        }
    }

    @Nested
    @DisplayName("shutdown (@PreDestroy)")
    class ShutdownTests {

        @Test
        @DisplayName("Позитивный тест: shutdown корректно останавливает executor")
        void shouldShutdownExecutorGracefully() throws Exception {
            Method shutdownMethod = TokenBlacklistServiceInMemoryImpl.class
                    .getDeclaredMethod("shutdown");
            shutdownMethod.setAccessible(true);

            assertDoesNotThrow(() -> shutdownMethod.invoke(tokenBlacklistService));

            Thread.sleep(100);

            tokenBlacklistService.blacklistToken(validToken, Duration.ofMinutes(1));
            assertTrue(tokenBlacklistService.isTokenBlacklisted(validToken));
        }

        @Test
        @DisplayName("Позитивный тест: shutdown можно вызывать многократно без ошибок")
        void shouldHandleMultipleShutdownCalls() throws Exception {
            Method shutdownMethod = TokenBlacklistServiceInMemoryImpl.class
                    .getDeclaredMethod("shutdown");
            shutdownMethod.setAccessible(true);

            assertDoesNotThrow(() -> {
                shutdownMethod.invoke(tokenBlacklistService);
                shutdownMethod.invoke(tokenBlacklistService);
            });
        }
    }

    @Nested
    @DisplayName("Пограничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Пустая строка как токен")
        void shouldHandleEmptyStringToken() {
            tokenBlacklistService.blacklistToken("", Duration.ofMinutes(1));
            assertTrue(tokenBlacklistService.isTokenBlacklisted(""));
        }

        @Test
        @DisplayName("Очень длинный токен")
        void shouldHandleVeryLongToken() {
            String longToken = "a".repeat(10000);
            tokenBlacklistService.blacklistToken(longToken, Duration.ofMinutes(1));
            assertTrue(tokenBlacklistService.isTokenBlacklisted(longToken));
        }

        @Test
        @DisplayName("TTL равный нулю")
        void shouldHandleZeroTtl() {
            tokenBlacklistService.blacklistToken(validToken, Duration.ZERO);
            assertFalse(tokenBlacklistService.isTokenBlacklisted(validToken),
                    "Токен с TTL=0 не должен считаться активным");
        }
    }
}