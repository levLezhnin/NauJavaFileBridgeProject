package ru.LevLezhnin.NauJava.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;

    @Autowired
    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponseDto getProfile() {
        return userService.getProfile();
    }

    @PatchMapping("/me")
    public void updateProfile(@RequestBody UpdateUserRequestDto updateUserRequestDto) {
        userService.updateUser(updateUserRequestDto);
    }

    @DeleteMapping("/me")
    public void deleteUser() {
        userService.deleteUser();
    }

    @GetMapping("/email")
    public Optional<User> findByEmail(@RequestParam("address") String email) {
        return userRepository.findByEmail(email);
    }

    @GetMapping("/username")
    public List<User> findByUsername(@RequestParam("like") String username,
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
