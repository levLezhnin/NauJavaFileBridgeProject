package ru.LevLezhnin.NauJava.exceptionhandling;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.dto.error.FieldError;
import ru.LevLezhnin.NauJava.dto.error.ValidationError;
import ru.LevLezhnin.NauJava.exceptions.*;
import ru.LevLezhnin.NauJava.exceptions.auth.InvalidTokenException;
import ru.LevLezhnin.NauJava.exceptions.auth.TokenExpiredException;
import ru.LevLezhnin.NauJava.exceptions.auth.TokenRevokedException;
import ru.LevLezhnin.NauJava.exceptions.common.EntityNotFoundException;
import ru.LevLezhnin.NauJava.exceptions.common.InvalidPasswordException;
import ru.LevLezhnin.NauJava.exceptions.common.SelfActionForbiddenException;
import ru.LevLezhnin.NauJava.exceptions.file.*;
import ru.LevLezhnin.NauJava.exceptions.user.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(MaxUploadSizeExceededException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Слишком большой файл",
                "Размер файла не должен превышать 100 МБ",
                httpServletRequest);
    }

    @ExceptionHandler(DownloadLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDownloadLimit(DownloadLimitExceededException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.GONE,
                "Достигнут лимит скачиваний файла",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(FileExpiredException.class)
    public ResponseEntity<ErrorResponse> handleFileExpired(FileExpiredException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.GONE,
                "Истекло время хранения файла",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Файл не найден",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(FileTooLargeException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Превышены ограничения на размер файла",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(FileUploadException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Не удалось загрузить файл, повторите попытку позже",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(IllegalFileAccessException.class)
    public ResponseEntity<ErrorResponse> handleIllegalFileAccess(IllegalFileAccessException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Доступ к файлу запрещён",
                e.getMessage(),
                httpServletRequest
        );
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
    public ResponseEntity<ValidationError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

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
    public ResponseEntity<ValidationError> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

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
        return buildResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Превышена квота",
                ex.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(UsernameTakenException.class)
    public ResponseEntity<ErrorResponse> handleUsernameTaken(UsernameTakenException ex, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Логин уже занят другим пользователем",
                ex.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLogin(InvalidLoginException ex, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Логин уже занят другим пользователем",
                ex.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(EmailTakenException.class)
    public ResponseEntity<ErrorResponse> handleEmailTaken(EmailTakenException ex, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "Email уже занят другим пользователем",
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
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Действие пользователя над самим собой запрещено",
                ex.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "У вас нет прав на выполнение этого действия",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserAlreadyBannedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyBanned(UserAlreadyBannedException ex, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Пользователь уже заблокирован",
                ex.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(UserNotBannedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotBanned(UserNotBannedException ex, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Пользователь не заблокирован",
                ex.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<ErrorResponse> handleTokenRevoked(TokenRevokedException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Токен отозван",
                e.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Истёк срок действия токена",
                e.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException e, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Невалидный токен",
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

    private ResponseEntity<ValidationError> buildValidationErrorResponse(List<FieldError> fieldErrors, HttpServletRequest request) {
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
