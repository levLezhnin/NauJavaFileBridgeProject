package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO запроса на обновление access-токена по refresh-токену.
 * <p>
 * Используется в {@code POST /api/v1/auth/refresh}.
 * Refresh-токен передаётся в теле (обычно извлекается из cookie на фронте).
 *
 * @param refreshToken валидный refresh JWT токен (обязательный, непустой)
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.api.AuthController#refresh(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
 * @see ru.LevLezhnin.NauJava.service.interfaces.AuthService#refresh(JwtRefreshRequestDto)
 */
public record JwtRefreshRequestDto(
        @NotBlank(message = "Refresh токен должен быть заполнен")
        String refreshToken) {}
