package ru.LevLezhnin.NauJava.dto.user;

public record UpdateUserRequestDto(String username, String password) {
    public boolean validate() {
        return containsUsername() || containsPassword();
    }

    public boolean containsUsername() {
        return !(username == null || username.isBlank());
    }

    public boolean containsPassword() {
        return !(password == null || password.isBlank());
    }
}
