package ru.LevLezhnin.NauJava.exception.auth;

public class TokenRevokedException extends RuntimeException {
    public TokenRevokedException(String message) {
        super(message);
    }
    public TokenRevokedException(String message, Throwable cause) {
        super(message, cause);
    }
}
