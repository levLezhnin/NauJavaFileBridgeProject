package ru.LevLezhnin.NauJava.exceptions.common;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
