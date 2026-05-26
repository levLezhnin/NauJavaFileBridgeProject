package ru.LevLezhnin.NauJava.exception.file;

public class DownloadLimitExceededException extends RuntimeException {
    public DownloadLimitExceededException(String message) {
        super(message);
    }
    public DownloadLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
