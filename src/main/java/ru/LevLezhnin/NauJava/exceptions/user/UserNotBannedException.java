package ru.LevLezhnin.NauJava.exceptions.user;

public class UserNotBannedException extends RuntimeException {
    public UserNotBannedException(String message) {
        super(message);
    }
    public UserNotBannedException(String message, Throwable cause) {
        super(message, cause);
    }
}
