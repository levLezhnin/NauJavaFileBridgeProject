package ru.LevLezhnin.NauJava.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Кастомный обработчик ошибок для веб-интерфейса (Thymeleaf).
 * <p>
 * Отдаёт красивые страницы ошибок (404, общая ошибка).
 *
 * @author Лев Лежнин
 */
@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String handleHtmlError(HttpServletRequest request, Model model) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", message != null && !message.isBlank() ? message : "Произошла ошибка.");
        model.addAttribute("requestUri", requestUri);

        if (statusCode != null && statusCode == 404) {
            return "error/404";
        }
        return "error/error";
    }
}
