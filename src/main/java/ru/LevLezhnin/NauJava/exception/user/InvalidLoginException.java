package ru.LevLezhnin.NauJava.exception.user;

public class InvalidLoginException extends RuntimeException {
    public InvalidLoginException(String message) {
        super(message);
    }
    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
