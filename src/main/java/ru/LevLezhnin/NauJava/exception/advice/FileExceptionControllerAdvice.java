package ru.LevLezhnin.NauJava.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.exception.file.*;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FileExceptionControllerAdvice extends AbstractControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(FileExceptionControllerAdvice.class);

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
        log.warn("Превышен лимит скачиваний. URI: {}, Детали: {}", httpServletRequest.getRequestURI(), e.getMessage());

        return buildResponse(
                HttpStatus.GONE,
                "Достигнут лимит скачиваний файла",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(FileExpiredException.class)
    public ResponseEntity<ErrorResponse> handleFileExpired(FileExpiredException e, HttpServletRequest httpServletRequest) {
        log.info("Попытка доступа к истёкшему файлу. URI: {}", httpServletRequest.getRequestURI());

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
        log.error("Не удалось загрузить файл пользователя. Причина: {}, URI: {}", e.getMessage(), httpServletRequest.getRequestURI(), e);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Не удалось загрузить файл, повторите попытку позже",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(IllegalFileAccessException.class)
    public ResponseEntity<ErrorResponse> handleIllegalFileAccess(IllegalFileAccessException e, HttpServletRequest httpServletRequest) {
        log.warn("Несанкционированная попытка доступа к файлу. URI: {}, IP: {}",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getRemoteAddr());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Доступ к файлу запрещён",
                e.getMessage(),
                httpServletRequest
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException ex, HttpServletRequest request) {

        log.error("Ошибка хранилища файлов. URI: {}, Сообщение: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ошибка работы с файлами",
                "Не удалось выполнить операцию с файлом. Пожалуйста, повторите позже.",
                request
        );
    }

}
