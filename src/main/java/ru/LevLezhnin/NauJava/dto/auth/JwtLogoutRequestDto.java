package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO запроса на выход из системы (logout).
 * <p>
 * Используется в {@code POST /api/v1/auth/logout}.
 * Токены обычно извлекаются из httpOnly cookies.
 *
 * @param accessToken  текущий access JWT (для отзыва) (обязательный, непустой)
 * @param refreshToken текущий refresh JWT (для отзыва) (обязательный, непустой)
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.api.AuthController#logout(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
 * @see ru.LevLezhnin.NauJava.service.interfaces.AuthService#logout(JwtLogoutRequestDto)
 */
public record JwtLogoutRequestDto(
        @NotBlank(message = "Access токен должен быть заполнен")
        String accessToken,
        @NotBlank(message = "Refresh токен должен быть заполнен")
        String refreshToken) {}
