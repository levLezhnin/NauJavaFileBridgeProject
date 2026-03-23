package ru.LevLezhnin.NauJava.exceptions;

public class FileStorageException extends RuntimeException {

    private final ErrorCodes errorCode;

    public ErrorCodes getErrorCode() {
        return errorCode;
    }

    public FileStorageException(String message, Throwable cause) {
        this(message, cause, ErrorCodes.GENERAL_FAILURE);
    }

    public FileStorageException(String message, Throwable cause, ErrorCodes errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static FileStorageException uploadFailed(String path, Throwable cause) {
        return new FileStorageException(
                "Не удалось загрузить файл по пути: " + path,
                cause,
                ErrorCodes.UPLOAD_FAILED);
    }

    public static FileStorageException downloadFailed(String path, Throwable cause) {
        return new FileStorageException(
                "Не удалось скачать файл по пути: " + path,
                cause,
                ErrorCodes.DOWNLOAD_FAILED
        );
    }

    public static FileStorageException deleteFailed(String path, Throwable cause) {
        return new FileStorageException(
                "Не удалось удалить файл по пути: " + path,
                cause,
                ErrorCodes.DELETE_FAILED
        );
    }

    public static FileStorageException fileNotFound(String path, Throwable cause) {
        return new FileStorageException(
                "Не найден файл по пути: " + path,
                cause,
                ErrorCodes.NOT_FOUND
        );
    }

    public enum ErrorCodes {
        GENERAL_FAILURE,
        UPLOAD_FAILED,
        DOWNLOAD_FAILED,
        DELETE_FAILED,
        NOT_FOUND
    }
}
