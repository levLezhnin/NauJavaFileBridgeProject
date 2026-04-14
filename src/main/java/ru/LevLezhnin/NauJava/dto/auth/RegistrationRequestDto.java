package ru.LevLezhnin.NauJava.dto.auth;

public record RegistrationRequestDto(String username, String email, String password) {
    public boolean validate() {
        return (username != null && !username.isBlank())
                && (email != null && !email.isBlank())
                && (password != null && !password.isBlank());
    }
}
