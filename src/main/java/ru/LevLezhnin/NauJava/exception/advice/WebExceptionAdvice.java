package ru.LevLezhnin.NauJava.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Обработчик исключений специально для веб-контроллеров (Thymeleaf-страницы).
 * <p>
 * Применяется только к классам, аннотированным {@link org.springframework.stereotype.Controller}
 * (в отличие от {@code @RestControllerAdvice}, который работает с REST-контроллерами).
 * <p>
 * Имеет самый низкий приоритет ({@code LOWEST_PRECEDENCE}), поэтому срабатывает после
 * более специфичных обработчиков.
 * <p>
 * Обрабатывает:
 * <ul>
 *   <li>Общие необработанные исключения - логирование + страница {@code error/error}</li>
 *   <li>{@link NoResourceFoundException} - страница 404</li>
 * </ul>
 *
 * @author Lev Lezhnin
 */
@ControllerAdvice(annotations = Controller.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class WebExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionAdvice.class);

    @ExceptionHandler(Exception.class)
    public String handleGenericWebException(Exception e, HttpServletRequest request, Model model) {
        log.error("Необработанное исключение в Web-контроллере. URI: {}, Причина: {}",
                request.getRequestURI(), e.getMessage(), e);

        model.addAttribute("statusCode", 500);
        model.addAttribute("errorMessage", "Произошла ошибка.");
        return "error/error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleWebNotFound(NoResourceFoundException e) {
        return "error/404";
    }
}
