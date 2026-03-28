package ru.LevLezhnin.NauJava.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.LevLezhnin.NauJava.dto.auth.JwtLoginRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtRefreshRequestDto;
import ru.LevLezhnin.NauJava.dto.auth.JwtResponseDto;
import ru.LevLezhnin.NauJava.dto.auth.RegistrationRequestDto;
import ru.LevLezhnin.NauJava.service.interfaces.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<JwtResponseDto> register(@RequestBody RegistrationRequestDto request) {
        JwtResponseDto response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDto> login(@RequestBody JwtLoginRequestDto request) {
        JwtResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseDto> refresh(@RequestBody JwtRefreshRequestDto request) {
        JwtResponseDto response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }
}
