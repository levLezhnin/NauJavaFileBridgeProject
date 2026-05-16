package ru.LevLezhnin.NauJava.exceptions.common;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
