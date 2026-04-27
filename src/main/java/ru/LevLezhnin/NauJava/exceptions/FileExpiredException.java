package ru.LevLezhnin.NauJava.exceptions;

public class FileExpiredException extends RuntimeException {
    public FileExpiredException(String message) {
        super(message);
    }
}
