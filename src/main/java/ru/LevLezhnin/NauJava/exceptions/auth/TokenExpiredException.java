package ru.LevLezhnin.NauJava.exceptions.auth;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
