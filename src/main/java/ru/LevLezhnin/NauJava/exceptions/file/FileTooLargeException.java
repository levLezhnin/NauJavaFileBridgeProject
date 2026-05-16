package ru.LevLezhnin.NauJava.exceptions.file;

public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(String message) {
        super(message);
    }
}
