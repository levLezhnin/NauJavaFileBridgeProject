package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO запроса аутентификации (login).
 * <p>
 * Используется в {@code POST /api/v1/auth/login}. Передаётся в теле запроса.
 *
 * @param username логин зарегистрированного пользователя (обязательный, непустой)
 * @param password пароль в открытом виде (обязательный, непустой)
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.api.AuthController#login(JwtLoginRequestDto, jakarta.servlet.http.HttpServletResponse)
 * @see ru.LevLezhnin.NauJava.service.interfaces.AuthService#login(JwtLoginRequestDto)
 */
public record JwtLoginRequestDto(
        @NotBlank(message = "Логин должен быть заполнен")
        String username,
        @NotBlank(message = "Пароль должен быть заполнен")
        String password) {}
