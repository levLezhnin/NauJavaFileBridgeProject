package ru.LevLezhnin.NauJava.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record JwtLogoutRequestDto(
        @NotBlank(message = "Access токен должен быть заполнен")
        String accessToken,
        @NotBlank(message = "Refresh токен должен быть заполнен")
        String refreshToken) {}
