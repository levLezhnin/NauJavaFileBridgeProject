package ru.LevLezhnin.NauJava.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/email")
    public Optional<User> findByEmail(@RequestBody String email) {
        return userRepository.findByEmail(email);
    }

    @GetMapping("/username")
    public List<User> findByUsername(@RequestBody String username,
                                     @RequestParam("page_size") int pageSize,
                                     @RequestParam("page") int page) {
        return userRepository.findByUsernameLikeIgnoreCase(username, PageRequest.of(page, pageSize));
    }

    @GetMapping("/all")
    public List<User> findAll(@RequestParam("page_size") int pageSize,
                              @RequestParam("page") int page) {
        return userRepository.findAllByOrderByIdAsc(PageRequest.of(page, pageSize));
    }
}
