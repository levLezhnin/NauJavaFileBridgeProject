package ru.LevLezhnin.NauJava.controller.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Контроллер для отдачи Thymeleaf-шаблонов веб-интерфейса.
 * <p>
 * Маршруты: главная, login, register, files, upload, profile и т.д.
 *
 * @author Лев Лежнин
 */
@Controller
public class ViewController {

    private static final Logger log = LoggerFactory.getLogger(ViewController.class);

    @GetMapping("/")
    public String homeView() {
        log.debug("Запрошена домашняя страница");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/files";
        }
        return "home";
    }

    @GetMapping("/login")
    public String loginView() {
        log.debug("Запрошена страница входа");
        return "login";
    }

    @GetMapping("/register")
    public String registerView() {
        log.debug("Запрошена страница регистрации");
        return "register";
    }

    @GetMapping("/forbidden")
    public String forbiddenView() {
        log.debug("Запрошена страница forbidden");
        return "forbidden";
    }

    @GetMapping("/files")
    public String filesView() {
        log.debug("Запрошена страница файлов пользователя");
        return "files";
    }

    @GetMapping("/upload")
    public String uploadView() {
        log.debug("Запрошена страница загрузки файла");
        return "upload";
    }

    @GetMapping("/profile")
    public String profileView() {
        log.debug("Запрошена страница профиля пользователя");
        return "profile";
    }

    @GetMapping("/download/{id}")
    public String downloadView(@PathVariable("id") String id, Model model) {
        log.debug("Запрошена страница скачивания файла");
        model.addAttribute("fileId", id);
        return "download";
    }

    @GetMapping("/notfound")
    public String notFoundView() {
        log.debug("Запрошена страница notfound");
        return "notfound";
    }

    @GetMapping("/admin/users/list")
    public String userListView() {
        log.debug("Запрошена админская страница пользователей сервиса");
        return "userList";
    }

    @GetMapping("/admin/files/list")
    public String adminFileListView() {
        log.debug("Запрошена админская страница файлов сервиса");
        return "adminFileList";
    }
}
