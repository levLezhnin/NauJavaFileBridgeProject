package ru.LevLezhnin.NauJava.exception.file;

public class IllegalFileAccessException extends RuntimeException {
    public IllegalFileAccessException(String message) {
        super(message);
    }
    public IllegalFileAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
