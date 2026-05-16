package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.LevLezhnin.NauJava.validation.user.UserBanReasonValid;

public record UserBanRequestDto(
        @NotNull(message = "ID пользователя для блокировки должен быть заполнен")
        @JsonProperty("ban_user_id")
        Long banUserId,
        @NotBlank(message = "Причина блокировки должна быть заполнена")
        @Size(min = 20, max = 500, message = "Причина блокировки пользователя должна быть длины от 20 до 500 символов")
        @UserBanReasonValid
        String reason
) {}
