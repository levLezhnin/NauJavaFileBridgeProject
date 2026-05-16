package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record JwtRefreshRequestDto(
        @NotBlank(message = "Refresh токен должен быть заполнен")
        String refreshToken) {}
