package ru.LevLezhnin.NauJava.dto.auth;

/**
 * DTO возврата токенов авторизации
 * @param accessToken   токен доступа (обязателен, непустой)
 * @param refreshToken  токен, для обновления токена доступа (обязателен, непустой)
 * @author Лев Лежнин
 */
public record JwtResponseDto(String accessToken, String refreshToken) {}
