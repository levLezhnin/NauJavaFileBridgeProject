package ru.LevLezhnin.NauJava.dto.user;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserProfileResponseDto(
        String username,
        String email,
        @JsonProperty("registered_at") Instant registeredAt,
        String role
) {}
