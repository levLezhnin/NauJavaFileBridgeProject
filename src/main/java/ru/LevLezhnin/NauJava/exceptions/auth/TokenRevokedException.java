package ru.LevLezhnin.NauJava.exceptions.auth;

public class TokenRevokedException extends RuntimeException {
    public TokenRevokedException(String message) {
        super(message);
    }
}
