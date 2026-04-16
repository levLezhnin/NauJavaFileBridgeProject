package ru.LevLezhnin.NauJava.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;

import java.util.List;

@Controller
public class ViewController {

    private final UserRepository userRepository;

    @Autowired
    public ViewController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String homeView() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/files";
        }
        return "home";
    }

    @GetMapping("/login")
    public String loginView() {
        return "login";
    }

    @GetMapping("/register")
    public String registerView() {
        return "register";
    }

    @GetMapping("/forbidden")
    public String forbiddenView() {
        return "forbidden";
    }

    @GetMapping("/files")
    public String filesView() {
        return "files";
    }

    @GetMapping("/upload")
    public String uploadView() {
        return "upload";
    }

    @GetMapping("/profile")
    public String profileView() {
        return "profile";
    }

    @GetMapping("/download/{id}")
    public String downloadView(@PathVariable("id") String id, Model model) {
        model.addAttribute("fileId", id);
        return "download";
    }

    @GetMapping("/admin/users/list")
    public String userListView(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "userList";
    }

    @GetMapping("/admin/report")
    public String reportView() {
        return "report";
    }

    @GetMapping("/admin/report/{id}")
    public String reportViewById(@PathVariable("id") Long id, Model model) {
        model.addAttribute("reportId", id);
        return "report";
    }
}
