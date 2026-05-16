package ru.LevLezhnin.NauJava.exceptions.file;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String message) {
        super(message);
    }
}
