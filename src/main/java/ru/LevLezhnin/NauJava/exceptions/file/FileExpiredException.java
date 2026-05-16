package ru.LevLezhnin.NauJava.exceptions.file;

public class FileExpiredException extends RuntimeException {
    public FileExpiredException(String message) {
        super(message);
    }
}
