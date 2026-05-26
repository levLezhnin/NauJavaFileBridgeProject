package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.LevLezhnin.NauJava.model.UserRole;

import java.time.Instant;

/**
 * DTO ответа с данными профиля пользователя.
 *
 * @param username      логин пользователя
 * @param email         email пользователя
 * @param registeredAt  дата регистрации пользователя
 * @param role          роль пользователя ({@link UserRole})
 * @author Лев Лежнин
 */
public record UserProfileResponseDto(
        String username,
        String email,
        @JsonProperty("registered_at") Instant registeredAt,
        String role
) {}
