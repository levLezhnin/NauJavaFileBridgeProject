package ru.LevLezhnin.NauJava.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.LevLezhnin.NauJava.dto.user.UpdateUserRequestDto;
import ru.LevLezhnin.NauJava.dto.user.UserProfileResponseDto;
import ru.LevLezhnin.NauJava.service.interfaces.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponseDto getProfile() {
        return userService.getProfile();
    }

    @PatchMapping("/me")
    public void updateProfile(@RequestBody @NotNull @Valid UpdateUserRequestDto updateUserRequestDto) {
        userService.updateUser(updateUserRequestDto);
    }

    @DeleteMapping("/me")
    public void deleteUser() {
        userService.deleteUser();
    }
}
