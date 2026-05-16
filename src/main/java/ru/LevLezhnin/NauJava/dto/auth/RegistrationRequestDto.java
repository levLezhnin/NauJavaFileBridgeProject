package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import ru.LevLezhnin.NauJava.validation.user.UserPasswordValid;
import ru.LevLezhnin.NauJava.validation.user.UsernameValid;

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
