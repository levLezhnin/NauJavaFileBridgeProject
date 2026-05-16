package ru.LevLezhnin.NauJava.exceptions.auth;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
