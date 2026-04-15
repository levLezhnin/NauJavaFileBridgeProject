package ru.LevLezhnin.NauJava.exceptionhandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import ru.LevLezhnin.NauJava.dto.ErrorResponse;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;

@ControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(value = Throwable.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Throwable e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера",
                "Произошла непредвиденная ошибка. Пожалуйста, повторите запрос позже",
                httpServletRequest
        );
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentExceptionHandler(IllegalArgumentException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                "Параметры запроса указаны неверно. Проверьте введённые данные.",
                httpServletRequest);
    }

    @ExceptionHandler(value = EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> entityNotFoundExceptionHandler(EntityNotFoundException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Сущность не найден",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResourceFoundExceptionHandler(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        if (request.getRequestURI().startsWith("/.well-known/")) {
            return ResponseEntity.notFound().build();
        }

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Ресурс не найден",
                "Запрашиваемый ресурс не найден или не существует",
                request);
    }

    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentialsExceptionHandler(BadCredentialsException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Ошибка аутентификации",
                "Неверный логин или пароль",
                httpServletRequest);
    }

    @ExceptionHandler(value = IllegalStateException.class)
    public ResponseEntity<ErrorResponse> illegalStateExceptionHandler(IllegalStateException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Некорректное состояние",
                e.getMessage(),
                httpServletRequest);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        status.value(),
                        error,
                        message,
                        request.getRequestURI()
                ));
    }
}
