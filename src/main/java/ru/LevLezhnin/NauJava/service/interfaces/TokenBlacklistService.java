package ru.LevLezhnin.NauJava.service.interfaces;

import java.time.Duration;

/**
 * Сервис для отзыва (blacklist) JWT токенов.
 * <p>
 * Используется при logout для инвалидации refresh и access токенов.
 * Хранит токены в памяти с TTL.
 *
 * @author Лев Лежнин
 */
public interface TokenBlacklistService {

    /**
     * Добавляет токен в in-memory blacklist на указанное время.
     * <p>
     * Контракт реализации:
     * <ul>
     *  <li>Токен хранится в виде криптографического хеша</li>
     *  <li>Алгоритм хеширования должен обеспечивать:
     *      <ul>
     *          <li>Односторонность: невозможность восстановления исходного токена по хешу</li>
     *          <li>Устойчивость к коллизиям в рамках ожидаемой нагрузки</li>
     *          <li>Детерминированность: одинаковый вход - одинаковый хеш</li>
     *      </ul>
     *  </li>
     *  <li>Конкретный алгоритм хеширования выносится в конфигурацию и не фиксируется в коде</li>
     *  <li>При {@code token == null} или {@code ttl == null || ttl.isNegative()} - метод молча игнорирует вызов (логирует warning)</li>
     *  <li>Фоновый поток каждые 5 минут удаляет просроченные записи</li>
     *  <li>При shutdown приложения поток корректно останавливается</li>
     * </ul>
     *
     * @param token JWT-токен (access или refresh)
     * @param ttl   оставшееся время жизни токена
     */
    void blacklistToken(String token, Duration ttl);

    /**
     * Проверяет, находится ли токен в blacklist (и не истёк ли он).
     * <p>
     * Контракт:
     * <ul>
     *   <li>{@code token == null} - всегда возвращает {@code false}</li>
     *   <li>Если запись найдена, но её expiry < now - запись удаляется и возвращается {@code false}</li>
     *   <li>Проверка и очистка просроченных записей происходит лениво при каждом вызове</li>
     * </ul>
     *
     * @param token JWT-токен
     * @return {@code true}, если токен отозван и ещё не истёк
     */
    boolean isTokenBlacklisted(String token);
}
