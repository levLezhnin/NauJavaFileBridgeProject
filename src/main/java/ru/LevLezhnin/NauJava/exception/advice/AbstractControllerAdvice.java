package ru.LevLezhnin.NauJava.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.error.FieldError;
import ru.LevLezhnin.NauJava.dto.error.ValidationError;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Базовый класс для всех @ControllerAdvice.
 * <p>
 * Содержит общие методы построения ErrorResponse и ValidationError.
 *
 * @author Лев Лежнин
 */
public abstract class AbstractControllerAdvice {

    protected ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
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

    protected ResponseEntity<ValidationError> buildValidationErrorResponse(List<FieldError> fieldErrors, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ValidationError(
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Запрос не прошёл валидацию",
                        "Ошибка валидации входных данных",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

}
