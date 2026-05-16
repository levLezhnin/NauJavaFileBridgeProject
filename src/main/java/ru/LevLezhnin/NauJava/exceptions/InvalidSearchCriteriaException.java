package ru.LevLezhnin.NauJava.exceptions;

public class InvalidSearchCriteriaException extends RuntimeException {
    public InvalidSearchCriteriaException(String message) {
        super(message);
    }
    public InvalidSearchCriteriaException(String message, Throwable cause) {
      super(message, cause);
  }
}
