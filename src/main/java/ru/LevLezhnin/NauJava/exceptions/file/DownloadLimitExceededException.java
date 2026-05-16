package ru.LevLezhnin.NauJava.exceptions.file;

public class DownloadLimitExceededException extends RuntimeException {
    public DownloadLimitExceededException(String message) {
        super(message);
    }
}
