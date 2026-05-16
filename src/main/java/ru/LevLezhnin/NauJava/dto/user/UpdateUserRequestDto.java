package ru.LevLezhnin.NauJava.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.LevLezhnin.NauJava.validation.user.UpdateUserRequestValid;
import ru.LevLezhnin.NauJava.validation.user.UserPasswordValid;
import ru.LevLezhnin.NauJava.validation.user.UsernameValid;

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
