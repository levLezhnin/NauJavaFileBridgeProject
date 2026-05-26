package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.LevLezhnin.NauJava.validation.user.*;

/**
 * DTO запроса на обновление данных аккаунт пользователем.
 * <p>
 * Используется в {@code PATCH /api/v1/users/me}. Передаётся в теле запроса.
 * <p>
 * Обязательно должно быть заполнено хотя бы одно из двух полей: {@code newUsername}, {@code newPassword}.
 * Если указан {@code newPassword}, то обязательно должны быть заполнены {@code currentPassword} и {@code confirmNewPassword}.
 * <p>
 * Предоставляет удобные методы проверки заполненноси конкретных полей.
 *
 * @param newUsername           новый логин пользователя (обязателен, валидируется {@link UsernameValidator}, если не указан newPassword)
 * @param newPassword           новый пароль пользователя (обязателен, валидируется {@link UserPasswordValidator}, если не указан newUsername)
 * @param currentPassword       подтверждение нового пароля пользователя (обязательный и непустой, если указан newPassword)
 * @param confirmNewPassword    подтверждение нового пароля пользователя (обязательный и непустой, если указан newPassword)
 *
 * @author Лев Лежнин
 * @see ru.LevLezhnin.NauJava.controller.api.UserController#updateProfile(UpdateUserRequestDto)
 * @see ru.LevLezhnin.NauJava.service.interfaces.UserService#updateUser(UpdateUserRequestDto)
 */

@UpdateUserRequestValid
public record UpdateUserRequestDto(
        @JsonProperty("new_username")
        @UsernameValid
        String newUsername,

        @JsonProperty("current_password")
        String currentPassword,

        @JsonProperty("new_password")
        @UserPasswordValid
        String newPassword,

        @JsonProperty("confirm_new_password")
        String confirmNewPassword) {

    private boolean isStringNotNullOrBlank(String s) {
        return !(s == null || s.isBlank());
    }

    public boolean containsNewUsername() {
        return isStringNotNullOrBlank(newUsername);
    }

    public boolean containsNewPassword() {
        return isStringNotNullOrBlank(newPassword);
    }

    public boolean containsCurrentPassword() {
        return isStringNotNullOrBlank(currentPassword);
    }

    public boolean containsConfirmNewPassword() {
        return isStringNotNullOrBlank(confirmNewPassword);
    }

}
