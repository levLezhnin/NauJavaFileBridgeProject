package ru.LevLezhnin.NauJava.exceptions.common;

public class SelfActionForbiddenException extends RuntimeException {
    public SelfActionForbiddenException(String message) {
        super(message);
    }
    public SelfActionForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
