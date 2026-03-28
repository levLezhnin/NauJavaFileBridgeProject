package ru.LevLezhnin.NauJava.exceptionhandling;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.LevLezhnin.NauJava.dto.ErrorResponse;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(value = Throwable.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Throwable e, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Внутренняя ошибка сервера",
                        e.getMessage(),
                        httpServletRequest.getRequestURI()));
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentExceptionHandler(IllegalArgumentException e, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        HttpStatus.BAD_REQUEST.value(),
                        "Неправильно составлен запрос",
                        e.getMessage(),
                        httpServletRequest.getRequestURI()));
    }

    @ExceptionHandler(value = EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> entityNotFoundExceptionHandler(EntityNotFoundException e, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        HttpStatus.NOT_FOUND.value(),
                        "Не найдено",
                        e.getMessage(),
                        httpServletRequest.getRequestURI()));
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResourceFoundExceptionHandler(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        if (request.getRequestURI().startsWith("/.well-known/")) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        HttpStatus.NOT_FOUND.value(),
                        "Не найден ресурс",
                        e.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentialsExceptionHandler(BadCredentialsException e, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        HttpStatus.BAD_REQUEST.value(),
                        "Неправильные данные аутентификации",
                        e.getMessage(),
                        httpServletRequest.getRequestURI()));
    }
}
