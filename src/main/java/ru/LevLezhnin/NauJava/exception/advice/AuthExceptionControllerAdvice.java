package ru.LevLezhnin.NauJava.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.LevLezhnin.NauJava.dto.error.ErrorResponse;
import ru.LevLezhnin.NauJava.exception.auth.InvalidTokenException;
import ru.LevLezhnin.NauJava.exception.auth.TokenExpiredException;
import ru.LevLezhnin.NauJava.exception.auth.TokenRevokedException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionControllerAdvice extends AbstractControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionControllerAdvice.class);

    @ExceptionHandler(value = BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentialsExceptionHandler(BadCredentialsException e, HttpServletRequest httpServletRequest) {

        log.warn("Попытка входа с неверными данными. URI: {}", httpServletRequest.getRequestURI());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Ошибка аутентификации",
                "Неверный логин или пароль",
                httpServletRequest);
    }

    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<ErrorResponse> handleTokenRevoked(TokenRevokedException e, HttpServletRequest httpServletRequest) {
        log.info("Попытка использования отозванного токена. URI: {}", httpServletRequest.getRequestURI());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Токен отозван",
                e.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(TokenExpiredException e, HttpServletRequest httpServletRequest) {
        log.info("Использован истёкший токен. URI: {}", httpServletRequest.getRequestURI());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Истёк срок действия токена",
                e.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException e, HttpServletRequest httpServletRequest) {
        log.warn("Получен невалидный токен. URI: {}", httpServletRequest.getRequestURI());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Невалидный токен",
                e.getMessage(),
                httpServletRequest);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException e, HttpServletRequest request) {
        log.info("Пользователь не авторизован. URI: {}", request.getRequestURI());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Пользователь не авторизован",
                e.getMessage(),
                request);
    }

}
