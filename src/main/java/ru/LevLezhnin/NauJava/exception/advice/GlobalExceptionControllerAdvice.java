package ru.LevLezhnin.NauJava.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.error.FieldError;
import ru.LevLezhnin.NauJava.dto.error.ValidationError;
import ru.LevLezhnin.NauJava.exception.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exception.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exception.common.InvalidSearchCriteriaException;
import ru.LevLezhnin.NauJava.exception.common.SelfActionForbiddenException;
import ru.LevLezhnin.NauJava.exception.storagequotas.StorageQuotaExceededException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для REST-контроллеров.
 * <p>
 * Преобразует исключения в структурированные {@link ErrorResponse}.
 * Имеет низкий приоритет (выполняется после специализированных advice).
 *
 * @author Лев Лежнин
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class GlobalExceptionControllerAdvice extends AbstractControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionControllerAdvice.class);

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception e, HttpServletRequest httpServletRequest) {

        log.error("Не найден обработчик для исключения. URI запроса: {}. Сообщение: {}", httpServletRequest.getRequestURI(), e.getMessage(), e);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера",
                "Произошла непредвиденная ошибка. Пожалуйста, повторите запрос позже",
                httpServletRequest
        );
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> httpMessageNotReadableExceptionHandler(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректный формат запроса",
                "Тело запроса должно содержать валидный JSON",
                request
        );
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentExceptionHandler(IllegalArgumentException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                e.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(value = EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> entityNotFoundExceptionHandler(EntityNotFoundException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Сущность не найдена",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(value = IllegalStateException.class)
    public ResponseEntity<ErrorResponse> illegalStateExceptionHandler(IllegalStateException e, HttpServletRequest httpServletRequest) {
        log.error("Некорректное состояние сервера, получен IllegalStateException. URI: {}, Сообщение: {}", httpServletRequest.getRequestURI(), e.getMessage(), e);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Некорректное состояние",
                e.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException e, HttpServletRequest httpServletRequest) {
       return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Неверный пароль",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldError(
                        err.getField(),
                        err.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        return buildValidationErrorResponse(fieldErrors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {

        List<FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> {
                    // Извлекаем имя поля из пути нарушения (например: "fileName" из "fileUploadRequestDto.fileName")
                    String propertyPath = violation.getPropertyPath().toString();
                    String fieldName = propertyPath.contains(".")
                            ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                            : propertyPath;

                    return new FieldError(
                            fieldName,
                            violation.getMessage()
                    );
                })
                .collect(Collectors.toList());

        return buildValidationErrorResponse(fieldErrors, request);
    }

    @ExceptionHandler(StorageQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleStorageQuotaExceeded(StorageQuotaExceededException ex, HttpServletRequest httpServletRequest) {
        log.warn("Превышена квота хранилища. URI: {}, Message: {}", httpServletRequest.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Превышена квота",
                ex.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(InvalidSearchCriteriaException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSearchCriteria(InvalidSearchCriteriaException ex, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Неверно задан критерий поиска",
                ex.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(SelfActionForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleSelfActionForbidden(SelfActionForbiddenException ex, HttpServletRequest httpServletRequest) {
        log.info("Попытка действия пользователя над самим собой. URI: {}, Message: {}", httpServletRequest.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Действие пользователя над самим собой запрещено",
                ex.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("AccessDenied: попытка доступа к ресурсу без соовтетствующих прав. URI: {}, IP: {}",
                request.getRequestURI(),
                request.getRemoteAddr());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "У вас нет прав на выполнение этого действия",
                ex.getMessage(),
                request
        );
    }
}
