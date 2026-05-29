package ru.LevLezhnin.NauJava.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.exception.user.*;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionControllerAdvice extends AbstractControllerAdvice {

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
                "Неверный логин",
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

    @ExceptionHandler(UserAlreadyBannedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyBanned(UserAlreadyBannedException ex, HttpServletRequest httpServletRequest) {
        return buildResponse(
                HttpStatus.CONFLICT,
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

}
