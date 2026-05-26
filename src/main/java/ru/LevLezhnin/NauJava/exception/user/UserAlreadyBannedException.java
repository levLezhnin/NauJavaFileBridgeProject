package ru.LevLezhnin.NauJava.exception.user;

public class UserAlreadyBannedException extends RuntimeException {
    public UserAlreadyBannedException(String message) {
        super(message);
    }
    public UserAlreadyBannedException(String message, Throwable cause) {
        super(message, cause);
    }
}
