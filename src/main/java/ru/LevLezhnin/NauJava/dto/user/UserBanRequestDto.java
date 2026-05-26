package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.LevLezhnin.NauJava.validation.user.UserBanReasonValid;

/**
 * DTO запроса на блокирование аккаунта пользователя.
 * <p>
 * Используется в {@code POST /api/v1/admin/users/ban}. Передаётся в теле запроса.
 * <p>
 * Требует обязательного заполнения ID аккаунта пользователя для блокировки и причины блокировки.
 *
 * @param banUserId       ID аккаунта блокируемого пользователя (обязательный)
 * @param reason          причина блокировки пользователя (обязательный, от 20 до 500 символов, валидируется {@link ru.LevLezhnin.NauJava.validation.user.UserBanReasonValidator})
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.admin.UserBanController#banUserById(UserBanRequestDto)
 * @see ru.LevLezhnin.NauJava.service.interfaces.UserBanService#banUserById(Long, String)
 */

public record UserBanRequestDto(
        @NotNull(message = "ID пользователя для блокировки должен быть заполнен")
        @JsonProperty("ban_user_id")
        Long banUserId,
        @NotBlank(message = "Причина блокировки должна быть заполнена")
        @Size(min = 20, max = 500, message = "Причина блокировки пользователя должна быть длины от 20 до 500 символов")
        @UserBanReasonValid
        String reason
) {}
