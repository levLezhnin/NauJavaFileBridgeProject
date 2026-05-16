package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UserProfileAdminResponseDto(
        Long id,
        String username,
        String email,
        String role,
        @JsonProperty("is_active") Boolean isActive,
        @JsonProperty("registered_at") Instant registeredAt
) {}
