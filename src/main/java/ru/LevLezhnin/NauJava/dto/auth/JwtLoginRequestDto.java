package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record JwtLoginRequestDto(
        @NotBlank(message = "Логин должен быть заполнен")
        String username,
        @NotBlank(message = "Пароль должен быть заполнен")
        String password) {}
