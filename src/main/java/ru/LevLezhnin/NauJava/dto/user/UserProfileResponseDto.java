package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UserProfileResponseDto(String username, String email, @JsonProperty("registered_at") Instant registeredAt) {}
