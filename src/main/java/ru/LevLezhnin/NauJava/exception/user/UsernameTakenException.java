package ru.LevLezhnin.NauJava.exception.user;

public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String message) {
        super(message);
    }
    public UsernameTakenException(String message, Throwable cause) {
        super(message, cause);
    }
}
