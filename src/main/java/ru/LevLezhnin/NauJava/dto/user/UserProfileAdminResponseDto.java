package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO ответа с подробными данными профиля пользователя для администраторов.
 * @param id            ID пользователя (обязательный, непустой)
 * @param username      Логин пользователя (обязательный, непустой)
 * @param email         Email пользователя (обязательный, непустой)
 * @param role          {@link ru.LevLezhnin.NauJava.model.UserRole} пользователя (обязательный, непустой)
 * @param isActive      Активен ли аккаунт пользователя в данный момент (обязательный, непустой)
 * @param registeredAt  Дата и время регистрации пользователя (обязательный, непустой)
 * @author Лев Лежнин
 */
public record UserProfileAdminResponseDto(
        Long id,
        String username,
        String email,
        String role,
        @JsonProperty("is_active") Boolean isActive,
        @JsonProperty("registered_at") Instant registeredAt
) {}
