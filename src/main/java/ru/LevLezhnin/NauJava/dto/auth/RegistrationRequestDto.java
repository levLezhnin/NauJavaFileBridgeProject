package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import ru.LevLezhnin.NauJava.validation.user.UserPasswordValid;
import ru.LevLezhnin.NauJava.validation.user.UsernameValid;

/**
 * DTO запроса регистрации нового пользователя.
 * <p>
 * Используется в {@code POST /api/v1/auth/register}. Передаётся в теле запроса.
 *
 * @param username логин пользователя (уникальный, обязателен, валидируется {@link ru.LevLezhnin.NauJava.validation.user.UsernameValidator})
 * @param email    адрес электронной почты (уникальный) (обязателен, непустой, валидируется {@link Email})
 * @param password пароль в открытом виде (будет захэширован) (обязателен, непустой, валиридуется {@link ru.LevLezhnin.NauJava.validation.user.UserPasswordValidator})
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.api.AuthController#register(RegistrationRequestDto, jakarta.servlet.http.HttpServletResponse)
 * @see ru.LevLezhnin.NauJava.service.interfaces.AuthService#register(RegistrationRequestDto)
 */
public record RegistrationRequestDto(
        @NotBlank(message = "Логин не может быть пустым")
        @UsernameValid
        String username,

        @NotBlank(message = "Email не может быть пустым")
        @Email
        String email,

        @NotBlank(message = "Пароль не может быть пустым")
        @UserPasswordValid
        String password) {}
