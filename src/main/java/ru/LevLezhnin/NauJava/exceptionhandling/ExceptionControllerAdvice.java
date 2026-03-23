package ru.LevLezhnin.NauJava.exceptionhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;

@ControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Exception exceptionHandler(Throwable e) {
        return new Exception(e.getMessage());
    }

    @ExceptionHandler(IllegalAccessException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Exception illegalArgumentExceptionHandler(IllegalArgumentException e) {
        return new Exception(e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Exception entityNotFoundExceptionHandler(EntityNotFoundException e) {
        return new Exception(e.getMessage());
    }
}
